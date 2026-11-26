/*
 * Este archivo define las etapas oficiales por las que pasa un pedido desde que se crea
 * hasta que se entrega o se cancela, así como el orden permitido para avanzar entre ellas.
 * Se utiliza en el seguimiento de Mis Pedidos y al actualizar compras en el panel de Pedidos.
 */
package com.tecnicsecure.entity;

public final class EstadoPedido {

    // Nombres oficiales de cada etapa por la que puede pasar un pedido
    public static final String PENDIENTE = "pendiente";
    public static final String CONFIRMADO = "confirmado";
    public static final String EN_PREPARACION = "en_preparacion";
    public static final String EMPACADO = "empacado";
    public static final String ENVIADO = "enviado";
    public static final String ENTREGADO = "entregado";
    public static final String CANCELADO = "cancelado";

    // Lista completa de todas las etapas válidas del pedido
    public static final java.util.List<String> TODOS = java.util.List.of(
            PENDIENTE, CONFIRMADO, EN_PREPARACION, EMPACADO, ENVIADO, ENTREGADO, CANCELADO);

    // Orden permitido para avanzar de una etapa a otra; se puede cancelar antes de ser entregado
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

    // Revisa si un nombre de etapa recibido existe dentro de las opciones oficiales
    public static boolean esValido(String estado) {
        return estado != null && TODOS.contains(estado.toLowerCase());
    }

    // Revisa si un pedido tiene permitido pasar de su etapa actual a una etapa nueva
    public static boolean permiteTransicion(String actual, String nuevo) {
        if (actual == null || nuevo == null) {
            return false;
        }
        return TRANSICIONES.getOrDefault(actual, java.util.List.of()).contains(nuevo);
    }
}
