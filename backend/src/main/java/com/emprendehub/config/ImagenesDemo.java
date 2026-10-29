package com.emprendehub.config;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Random;
import javax.imageio.ImageIO;
import org.springframework.web.multipart.MultipartFile;

/**
 * Las fotos de los datos de prueba, dibujadas al sembrar.
 *
 * <p>{@code uploads/} no está en git, así que las imágenes no pueden venir del
 * repositorio. Se dibujan con Java2D, que viene con el JDK: un fondo del color
 * de la categoría y la inicial del negocio o del producto. Sin ficheros
 * binarios versionados y sin licencias de por medio.
 *
 * <p>Se entregan como {@link MultipartFile} para entrar por los mismos servicios
 * que una subida desde el navegador, con sus mismas reglas de tipo y tamaño (B9).
 */
final class ImagenesDemo {

    /** Un color oscuro por categoría, para que la inicial blanca se lea. */
    private static final Map<String, Color> COLORES = Map.ofEntries(
            Map.entry("Gastronomía", new Color(0xB4, 0x53, 0x09)),
            Map.entry("Moda", new Color(0x9D, 0x17, 0x4D)),
            Map.entry("Tecnología", new Color(0x1D, 0x4E, 0xD8)),
            Map.entry("Belleza", new Color(0xA2, 0x1C, 0xAF)),
            Map.entry("Artesanías", new Color(0x92, 0x40, 0x0E)),
            Map.entry("Salud y bienestar", new Color(0x0F, 0x76, 0x6E)),
            Map.entry("Educación", new Color(0x43, 0x38, 0xCA)),
            Map.entry("Hogar", new Color(0x4D, 0x7C, 0x0F)),
            Map.entry("Finanzas", new Color(0x33, 0x41, 0x55)),
            Map.entry("Deportes", new Color(0xC2, 0x41, 0x0C)),
            Map.entry("Mascotas", new Color(0x7C, 0x2D, 0x12)),
            Map.entry("Eventos", new Color(0xBE, 0x12, 0x3C)));

    private ImagenesDemo() {
    }

    /**
     * Una foto de la galería: 960×720 con la inicial del negocio. Cada
     * {@code variante} reparte de otra forma los círculos del fondo, para que
     * las fotos de una misma galería no sean idénticas.
     */
    static MultipartFile deGaleria(String categoria, String nombreNegocio, int variante) {
        Color base = COLORES.getOrDefault(categoria, Color.DARK_GRAY);
        BufferedImage imagen = new BufferedImage(960, 720, BufferedImage.TYPE_INT_RGB);
        Graphics2D lienzo = preparar(imagen);

        lienzo.setColor(base);
        lienzo.fillRect(0, 0, 960, 720);
        Random azar = new Random(nombreNegocio.hashCode() * 31L + variante);
        lienzo.setColor(mezclar(base, Color.WHITE, 0.15));
        for (int i = 0; i < 5; i++) {
            int radio = 100 + azar.nextInt(200);
            lienzo.fillOval(azar.nextInt(960) - radio, azar.nextInt(720) - radio,
                    radio * 2, radio * 2);
        }
        centrar(lienzo, inicial(nombreNegocio), Color.WHITE, 340, 960, 720);

        lienzo.dispose();
        return comoArchivo(imagen, "galeria-" + variante + ".png");
    }

    /** La foto de un producto: 600×600, el tono claro de la categoría y la inicial. */
    static MultipartFile deProducto(String categoria, String nombreProducto) {
        Color base = COLORES.getOrDefault(categoria, Color.DARK_GRAY);
        BufferedImage imagen = new BufferedImage(600, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D lienzo = preparar(imagen);

        lienzo.setColor(mezclar(base, Color.WHITE, 0.82));
        lienzo.fillRect(0, 0, 600, 600);
        lienzo.setColor(mezclar(base, Color.WHITE, 0.6));
        lienzo.fillOval(110, 110, 380, 380);
        centrar(lienzo, inicial(nombreProducto), base, 230, 600, 600);

        lienzo.dispose();
        return comoArchivo(imagen, "producto.png");
    }

    private static Graphics2D preparar(BufferedImage imagen) {
        Graphics2D lienzo = imagen.createGraphics();
        lienzo.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        lienzo.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return lienzo;
    }

    private static void centrar(Graphics2D lienzo, String texto, Color color, int tamano,
                                int ancho, int alto) {
        lienzo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, tamano));
        lienzo.setColor(color);
        FontMetrics medidas = lienzo.getFontMetrics();
        int x = (ancho - medidas.stringWidth(texto)) / 2;
        int y = (alto - medidas.getHeight()) / 2 + medidas.getAscent();
        lienzo.drawString(texto, x, y);
    }

    /** La primera letra del nombre, en mayúscula. */
    private static String inicial(String nombre) {
        return nombre.substring(0, 1).toUpperCase();
    }

    /** {@code proporcion} 0 deja el color tal cual; 1 lo convierte en el otro. */
    private static Color mezclar(Color color, Color otro, double proporcion) {
        return new Color(
                (int) Math.round(color.getRed() * (1 - proporcion) + otro.getRed() * proporcion),
                (int) Math.round(color.getGreen() * (1 - proporcion)
                        + otro.getGreen() * proporcion),
                (int) Math.round(color.getBlue() * (1 - proporcion) + otro.getBlue() * proporcion));
    }

    private static MultipartFile comoArchivo(BufferedImage imagen, String nombre) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try {
            ImageIO.write(imagen, "png", salida);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo dibujar la imagen de prueba", e);
        }
        return new ImagenGenerada(nombre, salida.toByteArray());
    }

    /**
     * Una imagen en memoria con la forma de un fichero subido.
     *
     * <p>Spring solo trae implementaciones para pruebas ({@code MockMultipartFile}
     * está en {@code spring-test}), así que se escribe aquí: son ocho métodos que
     * devuelven lo que ya se tiene.
     */
    private record ImagenGenerada(String nombre, byte[] contenido) implements MultipartFile {

        @Override
        public String getName() {
            return "archivo";
        }

        @Override
        public String getOriginalFilename() {
            return nombre;
        }

        @Override
        public String getContentType() {
            return "image/png";
        }

        @Override
        public boolean isEmpty() {
            return contenido.length == 0;
        }

        @Override
        public long getSize() {
            return contenido.length;
        }

        @Override
        public byte[] getBytes() {
            return contenido;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(contenido);
        }

        @Override
        public void transferTo(File destino) throws IOException {
            Files.write(destino.toPath(), contenido);
        }
    }
}
