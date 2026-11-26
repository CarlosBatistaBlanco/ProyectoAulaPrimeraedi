package com.tecniservicios.tecnisecure.data.api

import com.tecniservicios.tecnisecure.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // --- AUTENTICACIÓN Y SEGURIDAD ---
    @GET("api/auth/captcha")
    suspend fun getCaptcha(): Response<CaptchaResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<TokenResponse>

    @POST("api/auth/registro")
    suspend fun registro(@Body request: RegistroRequest): Response<Map<String, Any>>

    @GET("api/auth/me")
    suspend fun getPerfilActual(): Response<UsuarioResponse>

    // --- CATÁLOGO DE PRODUCTOS ---
    @GET("api/productos")
    suspend fun getProductos(
        @Query("texto") texto: String? = null,
        @Query("idCategoria") idCategoria: Int? = null,
        @Query("pagina") pagina: Int = 0,
        @Query("tamanio") tamanio: Int = 100
    ): Response<PageResponse<Producto>>

    @GET("api/productos/{id}")
    suspend fun getProductoDetalle(@Path("id") id: Int): Response<Producto>

    @GET("api/servicios")
    suspend fun getServicios(): Response<List<ServicioTecnico>>

    // --- PEDIDOS, PAGOS Y FACTURACIÓN DIAN ---
    @POST("api/pedidos")
    suspend fun crearPedido(@Body request: PedidoRequest): Response<PedidoResponse>

    @GET("api/pedidos/mis-pedidos")
    suspend fun getMisPedidos(
        @Query("pagina") pagina: Int = 0,
        @Query("tamanio") tamanio: Int = 50
    ): Response<PageResponse<PedidoResponse>>

    @GET("api/pedidos")
    suspend fun getTodosLosPedidos(
        @Query("pagina") pagina: Int = 0,
        @Query("tamanio") tamanio: Int = 50
    ): Response<PageResponse<PedidoResponse>>

    @GET("api/pedidos/{id}/factura")
    suspend fun getFacturaPedido(@Path("id") id: Int): Response<FacturaDetalleResponse>

    @POST("api/pedidos/{id}/pagar")
    suspend fun pagarPedido(
        @Path("id") id: Int,
        @Body request: PagoPedidoRequest
    ): Response<Map<String, Any>>

    // --- DIRECCIONES DE ENTREGA (MÁX. 15) ---
    @GET("api/direcciones")
    suspend fun getMisDirecciones(): Response<List<Direccion>>

    @POST("api/direcciones")
    suspend fun crearDireccion(@Body request: DireccionRequest): Response<Direccion>

    @PUT("api/direcciones/{id}")
    suspend fun actualizarDireccion(
        @Path("id") id: Int,
        @Body request: DireccionRequest
    ): Response<Direccion>

    @PATCH("api/direcciones/{id}/predeterminada")
    suspend fun marcarDireccionPredeterminada(@Path("id") id: Int): Response<Direccion>

    @DELETE("api/direcciones/{id}")
    suspend fun eliminarDireccion(@Path("id") id: Int): Response<Unit>

    // --- TAREAS OPERATIVAS V2.0 (EMPLEADO Y ADMIN) ---
    @GET("api/gestion-empleados/mis-tareas")
    suspend fun getMisTareas(): Response<List<TareaEmpleado>>

    @GET("api/gestion-empleados/tareas")
    suspend fun getTodasLasTareas(): Response<List<TareaEmpleado>>

    @GET("api/gestion-empleados/empleados")
    suspend fun getEmpleados(): Response<List<EmpleadoResumen>>

    @POST("api/gestion-empleados/tareas")
    suspend fun asignarTarea(@Body request: AsignarTareaRequest): Response<TareaEmpleado>

    @PATCH("api/gestion-empleados/tareas/{id}/progreso")
    suspend fun actualizarProgresoTarea(
        @Path("id") id: Int,
        @Body request: ProgresoTareaRequest
    ): Response<TareaEmpleado>

    @PATCH("api/gestion-empleados/tareas/{id}/calificar")
    suspend fun calificarTarea(
        @Path("id") id: Int,
        @Body request: CalificarTareaRequest
    ): Response<TareaEmpleado>

    @GET("api/gestion-empleados/tareas/{id}/historial")
    suspend fun getHistorialTarea(@Path("id") id: Int): Response<List<TareaHistorialEntrega>>

    // --- ASISTENTE VIRTUAL IA Y CHAT CON ASESOR ---
    @POST("api/asistente")
    suspend fun conversarAsistente(@Body request: AsistenteRequest): Response<AsistenteResponse>

    @GET("api/asistente/conversacion/{token}/mensajes")
    suspend fun getMensajesConversacion(@Path("token") token: String): Response<List<MensajeVisitanteResponse>>

    @POST("api/asistente/mensajes")
    suspend fun enviarMensajeAsesor(@Body request: MensajeVisitanteRequest): Response<ConversacionVisitanteResponse>
}
