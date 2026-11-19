package com.tecnicsecure.entity;

/** Estados validos del pedido (coinciden con el ENUM de la BD). */
public final class EstadoPedido {

    public static final String PENDIENTE = "pendiente";
    public static final String CONFIRMADO = "confirmado";
    public static final String EN_PREPARACION = "en_preparacion";
    public static final String EMPACADO = "empacado";
    public static final String ENVIADO = "enviado";
    public static final String ENTREGADO = "entregado";
    public static final String CANCELADO = "cancelado";

    public static final java.util.List<String> TODOS = java.util.List.of(
            PENDIENTE, CONFIRMADO, EN_PREPARACION, EMPACADO, ENVIADO, ENTREGADO, CANCELADO);

    /** Flujo lineal permitido; 'cancelado' se permite desde cualquier estado no entregado. */
    private static final java.util.Map<String, java.util.List<String>> TRANSICIONES = java.util.Map.of(
            PENDIENTE, java.util.List.of(CONFIRMADO, CANCELADO),
            CONFIRMADO, java.util.List.of(EN_PREPARACION, CANCELADO),
            EN_PREPARACION, java.util.List.of(EMPACADO, CANCELADO),
            EMPACADO, java.util.List.of(ENVIADO, CANCELADO),
            ENVIADO, java.util.List.of(ENTREGADO),
            ENTREGADO, java.util.List.of(),
            CANCELADO, java.util.List.of());

    private EstadoPedido() {
    }

    public static boolean esValido(String estado) {
        return estado != null && TODOS.contains(estado.toLowerCase());
    }

    public static boolean permiteTransicion(String actual, String nuevo) {
        if (actual == null || nuevo == null) {
            return false;
        }
        return TRANSICIONES.getOrDefault(actual, java.util.List.of()).contains(nuevo);
    }
}
