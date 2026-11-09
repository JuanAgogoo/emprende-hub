#!/usr/bin/env python3
"""Aplica fotos reales y textos a los 40 negocios de demostración.

No borra negocios, cuentas, opiniones ni consultas. Copia las fotos al volumen
y actualiza la base en UNA transacción, que se cancela si algo no es lo
esperado. Se puede correr varias veces sin duplicar nada.

    python3 deploy/aplicar-demo.py --destino local        # copia desechable ehlocal
    python3 deploy/aplicar-demo.py --destino produccion
    python3 deploy/aplicar-demo.py --destino produccion --limpiar   # al final

Las fotos de letras que quedan sin uso solo se borran del disco con --limpiar,
después de comprobar el resultado. Hasta entonces, el backup basta para volver.
"""
import argparse
import json
import os
import pathlib
import re
import subprocess

RAIZ = pathlib.Path(__file__).resolve().parent.parent
FOTOS = RAIZ / 'deploy/fotos-demo'
C = json.loads((RAIZ / 'deploy/contenido-demo.json').read_text(encoding='utf-8'))
PATRON = r'^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.png$'
SERVIDOR = ['ssh', '-i', os.path.expanduser('~/.ssh/emprendehub_vps'),
            '-o', 'BatchMode=yes', 'deploy@2.24.113.157']
PSQL = ('exec -T postgres sh -c \'psql -q -v ON_ERROR_STOP=1 -At -F "|" '
        '-U "$POSTGRES_USER" -d "$POSTGRES_DB"\'')


def lit(valor):
    return 'NULL' if valor is None else "'" + str(valor).replace("'", "''") + "'"


class Destino:
    def __init__(self, nombre):
        self.nombre = nombre
        self.remoto = nombre == 'produccion'
        if self.remoto:
            self.volumen = 'emprendehub-prod_fotos'
            self.compose = 'cd /opt/emprendehub && docker compose -f docker-compose.prod.yml'
        else:
            self.volumen = 'ehlocal_fotos'
            self.compose = (f'cd {RAIZ} && docker compose -p ehlocal '
                            f'--env-file {os.environ["EHLOCAL_ENV"]} -f docker-compose.prod.yml')

    def ejecutar(self, orden, entrada=None):
        cmd = SERVIDOR + [orden] if self.remoto else ['bash', '-c', orden]
        r = subprocess.run(cmd, input=entrada, capture_output=True)
        if r.returncode:
            raise SystemExit(f'Falló, no se aplicó esta parte:\n{r.stderr.decode(errors="replace")[-900:]}')
        return r.stdout

    def sql(self, texto):
        return self.ejecutar(f'{self.compose} {PSQL}', texto.encode('utf-8')).decode('utf-8')


def archivos_negocio(n):
    return [f"demo-n{n['id']:02d}-{i}-{p}.jpg" for i, p in enumerate(n['fotos'], 1)]


def construir_sql():
    negocios = C['negocios']
    ids = ','.join(str(n['id']) for n in negocios)
    textos = ',\n'.join(
        f"({n['id']},{lit(n['antes'])},{lit(n['nombre'])},{lit(n['descripcion'])},{lit(n['barrio'])})"
        for n in negocios)
    fotos = ',\n'.join(f"({n['id']},{lit(a)},{o})"
                       for n in negocios for o, a in enumerate(archivos_negocio(n)))
    productos = ',\n'.join(f"({lit(k)},{lit(f'demo-p-{v}.jpg')})" for k, v in C['productos'].items())
    patron = lit(PATRON)
    return f"""begin;
create temp table t_negocio(id bigint, antes text, nombre text, descripcion text, barrio text) on commit drop;
insert into t_negocio values {textos};
create temp table t_foto(negocio_id bigint, archivo text, orden int) on commit drop;
insert into t_foto values {fotos};
create temp table t_producto(nombre text, archivo text) on commit drop;
insert into t_producto values {productos};
do $$ declare n int; begin
  select count(*) into n from negocio x join t_negocio t on t.id=x.id where x.nombre in (t.antes,t.nombre);
  if n<>40 then raise exception 'esperaba 40 negocios conocidos, hay %', n; end if;
  select count(*) into n from t_negocio t join negocio x on x.id=t.id
    where t.barrio is not null and not exists (select 1 from barrio b where b.nombre=t.barrio and b.ciudad_id=x.ciudad_id);
  if n<>0 then raise exception 'barrios que no son de su ciudad: %', n; end if;
  select count(*) into n from foto f where f.negocio_id in ({ids}) and f.nombre_archivo !~ {patron} and f.nombre_archivo not like 'demo-%';
  if n<>0 then raise exception 'hay % fotos que no son de la semilla', n; end if;
  select count(*) into n from producto p where p.negocio_id in ({ids}) and p.nombre not in (select nombre from t_producto);
  if n<>0 then raise exception 'hay % productos sin foto asignada', n; end if;
end $$;
select 'VIEJA|'||nombre_archivo from foto where negocio_id in ({ids}) and nombre_archivo ~ {patron};
select 'VIEJA|'||foto from producto where negocio_id in ({ids}) and foto ~ {patron};
update negocio x set nombre=t.nombre, descripcion=t.descripcion,
  barrio_id=coalesce((select b.id from barrio b where b.nombre=t.barrio and b.ciudad_id=x.ciudad_id), x.barrio_id)
  from t_negocio t where t.id=x.id;
delete from foto where negocio_id in ({ids}) and nombre_archivo ~ {patron};
delete from foto f where f.negocio_id in ({ids}) and f.nombre_archivo like 'demo-%'
  and not exists (select 1 from t_foto t where t.negocio_id=f.negocio_id and t.archivo=f.nombre_archivo);
update foto f set orden=t.orden from t_foto t where t.negocio_id=f.negocio_id and t.archivo=f.nombre_archivo;
insert into foto(negocio_id,nombre_archivo,orden,estado,fecha_subida)
  select t.negocio_id,t.archivo,t.orden,'APROBADA',now() from t_foto t
  where not exists (select 1 from foto f where f.negocio_id=t.negocio_id and f.nombre_archivo=t.archivo);
update producto p set foto=t.archivo from t_producto t
  where p.negocio_id in ({ids}) and p.nombre=t.nombre and (p.foto ~ {patron} or p.foto like 'demo-%');
select 'TOTAL|negocios='||(select count(*) from negocio)||' usuarios='||(select count(*) from usuario)
  ||' opiniones='||(select count(*) from opinion)||' consultas='||(select count(*) from consulta)
  ||' fotos='||(select count(*) from foto)||' productos='||(select count(*) from producto)
  ||' letras='||(select count(*) from foto where nombre_archivo ~ {patron})
  ||' letras_producto='||(select count(*) from producto where foto ~ {patron});
commit;
"""


def main():
    a = argparse.ArgumentParser()
    a.add_argument('--destino', choices=['local', 'produccion'], required=True)
    a.add_argument('--limpiar', action='store_true')
    args = a.parse_args()
    d = Destino(args.destino)
    lista = RAIZ / f'deploy/fotos-demo-viejas-{args.destino}.txt'
    previas = set(lista.read_text().split()) if lista.exists() else set()
    docker = f'docker run --rm -i --network none -v {d.volumen}:/fotos alpine:3.22'

    if args.limpiar:
        nombres = sorted(n for n in previas if re.fullmatch(PATRON, n))
        if nombres:
            comp = ','.join(lit(n) for n in nombres)
            usadas = d.sql(f'select count(*) from foto where nombre_archivo in ({comp}); '
                           f'select count(*) from producto where foto in ({comp});').split()
            if usadas != ['0', '0']:
                raise SystemExit(f'Aún hay fotos en uso ({usadas}); no se borra ninguna.')
            d.ejecutar(f"{docker} sh -c 'cd /fotos && rm -f -- {' '.join(nombres)}'")
        print(f'Borradas {len(nombres)} fotos de letras.')
        return

    jpgs = sorted(p.name for p in FOTOS.glob('demo-*.jpg'))
    paquete = subprocess.run(['tar', '--owner=1001', '--group=1001', '-C', str(FOTOS), '-cf', '-'] + jpgs,
                             capture_output=True, check=True).stdout
    d.ejecutar(f"{docker} tar -xf - -C /fotos", paquete)
    print(f'Copiadas {len(jpgs)} fotos.')
    salida = d.sql(construir_sql())
    viejas = [l.split('|', 1)[1] for l in salida.splitlines() if l.startswith('VIEJA|')]
    lista.write_text('\n'.join(sorted(previas | set(viejas))) + '\n')
    print(f'Fotos de letras retiradas de la base en esta corrida: {len(viejas)}')
    print(next(l for l in salida.splitlines() if l.startswith('TOTAL|'))[6:])


if __name__ == '__main__':
    main()
