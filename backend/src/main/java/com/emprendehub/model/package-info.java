/**
 * Entidades JPA ({@code @Entity}).
 *
 * <p>El esquema lo genera Hibernate a partir de estas clases
 * ({@code ddl-auto: update}), igual que en el taller de CRUD del curso.
 *
 * <p>Llevan Lombok granular ({@code @Getter}, {@code @Setter},
 * {@code @NoArgsConstructor}) porque JPA necesita constructor vacío y
 * mutabilidad, así que no pueden ser records. <strong>Nunca {@code @Data}</strong>:
 * la presentación de la semana 2 lo desaconseja expresamente.
 */
package com.emprendehub.model;
