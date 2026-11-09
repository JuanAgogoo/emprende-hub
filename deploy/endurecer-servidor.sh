#!/usr/bin/env bash
# Endurece el servidor. Se ejecuta DESDE EL PORTÁTIL, una vez:
#
#   ./deploy/endurecer-servidor.sh
#
# Actualiza el sistema, crea el usuario `deploy` con tu misma llave, activa UFW
# (22, 80, 443 y 443/udp) y fail2ban, y deja SSH solo con llave.
#
# Seguro frente a quedarse fuera: mantiene abierta una sesión de root durante
# todo el proceso, valida con `sshd -t` antes de recargar, y prueba una conexión
# NUEVA como `deploy`. Si no entra o sudo falla, deshace el cambio de SSH.
set -euo pipefail

SERVIDOR="${SERVIDOR:-2.24.113.157}"
LLAVE="${DEPLOY_LLAVE:-$HOME/.ssh/emprendehub_vps}"
SOCKET="$(mktemp -u /tmp/endurecer-XXXXXX)"
ROOT=(ssh -i "$LLAVE" -S "$SOCKET" "root@$SERVIDOR")
DROPIN=/etc/ssh/sshd_config.d/00-emprendehub.conf

echo "== Abriendo la sesión de root que se queda abierta"
ssh -i "$LLAVE" -o ConnectTimeout=15 -M -N -f -S "$SOCKET" "root@$SERVIDOR"
trap 'ssh -S "$SOCKET" -O exit "root@$SERVIDOR" 2>/dev/null || true' EXIT

echo "== Actualizando el sistema, usuario, firewall y fail2ban"
"${ROOT[@]}" 'bash -s' <<'REMOTO'
set -euo pipefail
export DEBIAN_FRONTEND=noninteractive
apt-get update -qq
apt-get upgrade -y -qq -o Dpkg::Options::=--force-confdef -o Dpkg::Options::=--force-confold
apt-get install -y -qq ufw fail2ban

id deploy >/dev/null 2>&1 || adduser --disabled-password --gecos "" deploy
usermod -aG sudo,docker deploy
install -d -m 700 -o deploy -g deploy /home/deploy/.ssh
install -m 600 -o deploy -g deploy /root/.ssh/authorized_keys /home/deploy/.ssh/authorized_keys
echo 'deploy ALL=(ALL) NOPASSWD:ALL' > /etc/sudoers.d/90-deploy
chmod 440 /etc/sudoers.d/90-deploy
visudo -cf /etc/sudoers.d/90-deploy >/dev/null

# La carpeta del proyecto pasa a ser de deploy, para desplegar con ese usuario.
if [ -d /opt/emprendehub ]; then chown -R deploy:deploy /opt/emprendehub; fi

ufw default deny incoming
ufw default allow outgoing
ufw allow 22/tcp
ufw allow 80/tcp
ufw allow 443/tcp
ufw allow 443/udp
ufw --force enable

cat > /etc/fail2ban/jail.d/sshd-emprendehub.local <<'JAIL'
[sshd]
enabled = true
maxretry = 5
findtime = 10m
bantime = 1h
JAIL
systemctl enable --now fail2ban
systemctl restart fail2ban
REMOTO

echo "== Dejando SSH solo con llave (validando antes de recargar)"
"${ROOT[@]}" "bash -s" <<REMOTO
set -euo pipefail
printf 'PasswordAuthentication no\nKbdInteractiveAuthentication no\nPermitRootLogin prohibit-password\n' > $DROPIN
if ! sshd -t; then rm -f $DROPIN; echo 'sshd -t falló: no se aplicó nada'; exit 1; fi
systemctl reload ssh
REMOTO

echo "== Probando una conexión NUEVA como deploy (la de root sigue abierta)"
NUEVA=(ssh -i "$LLAVE" -o ControlPath=none -o BatchMode=yes -o ConnectTimeout=15 "deploy@$SERVIDOR")
if "${NUEVA[@]}" 'sudo -n true && docker ps >/dev/null && echo "deploy entra, sudo y docker funcionan"'; then
  "${ROOT[@]}" "sshd -T | grep -E '^(passwordauthentication|permitrootlogin) '; ufw status | head -8; fail2ban-client status sshd | head -3"
  echo
  echo "LISTO. Para desplegar con el usuario nuevo:"
  echo "  DEPLOY_DESTINO=deploy@$SERVIDOR ./deploy/deploy.sh"
else
  echo "deploy NO pudo entrar: deshaciendo el cambio de SSH." >&2
  "${ROOT[@]}" "rm -f $DROPIN && sshd -t && systemctl reload ssh"
  exit 1
fi
