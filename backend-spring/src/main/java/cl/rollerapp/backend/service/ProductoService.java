package cl.rollerapp.backend.service;

import cl.rollerapp.backend.dto.producto.CrearProductoRequest;
import cl.rollerapp.backend.dto.producto.ProductoResponse;
import cl.rollerapp.backend.exception.ApiException;
import cl.rollerapp.backend.model.Mecanismo;
import cl.rollerapp.backend.model.Producto;
import cl.rollerapp.backend.model.AuditoriaAdministrativa;
import cl.rollerapp.backend.repository.AuditoriaAdministrativaRepository;
import cl.rollerapp.backend.model.Tela;
import cl.rollerapp.backend.repository.MecanismoRepository;
import cl.rollerapp.backend.repository.ProductoRepository;
import cl.rollerapp.backend.repository.TelaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final AuditoriaAdministrativaRepository auditoriaRepository;
    private final TelaRepository telaRepository;
    private final MecanismoRepository mecanismoRepository;

    @Transactional(readOnly = true)
    public List<ProductoResponse> listarActivos() {
        return productoRepository.findByActivoTrue().stream().map(ProductoResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> listarTodos() {
        return productoRepository.findAll().stream().map(ProductoResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtenerPorId(Long id) {
        return ProductoResponse.desde(buscarOFallar(id));
    }

    @Transactional
    public ProductoResponse crear(CrearProductoRequest req, Long actorId) {
        Producto producto = Producto.builder()
                .nombre(req.nombre())
                .descripcion(req.descripcion())
                .categoria(req.categoria())
                .imagenPrincipal(req.imagenPrincipal())
                .anchoMaxCm(req.anchoMaxCm() != null ? req.anchoMaxCm() : new BigDecimal("200"))
                .altoMaxCm(req.altoMaxCm() != null ? req.altoMaxCm() : new BigDecimal("260"))
                .activo(true)
                .build();

        if (req.telaDefectoId() != null) {
            producto.setTelaDefecto(telaRepository.findById(req.telaDefectoId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "La tela indicada no existe.")));
        }
        if (req.mecanismoDefectoId() != null) {
            producto.setMecanismoDefecto(mecanismoRepository.findById(req.mecanismoDefectoId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "El mecanismo indicado no existe.")));
        }

        producto = productoRepository.save(producto);
        registrarCambio(actorId, "CREAR_PRODUCTO", producto.getId(), "Producto creado: " + producto.getNombre());
        return ProductoResponse.desde(producto);
    }

    @Transactional
    public ProductoResponse editar(Long id, CrearProductoRequest req, Long actorId) {
        Producto producto = buscarOFallar(id);
        List<String> cambios = new ArrayList<>();
        if (!Objects.equals(producto.getNombre(), req.nombre())) {
            cambios.add("Nombre: '" + producto.getNombre() + "' → '" + req.nombre() + "'");
        }
        if (!Objects.equals(producto.getDescripcion(), req.descripcion())) cambios.add("Descripción modificada");
        if (!Objects.equals(producto.getCategoria(), req.categoria())) cambios.add("Categoría: " + producto.getCategoria() + " → " + req.categoria());
        if (!Objects.equals(producto.getImagenPrincipal(), req.imagenPrincipal())) cambios.add("Imagen principal modificada");
        if (req.anchoMaxCm() != null && producto.getAnchoMaxCm().compareTo(req.anchoMaxCm()) != 0)
            cambios.add("Ancho máximo: " + producto.getAnchoMaxCm() + " → " + req.anchoMaxCm() + " cm");
        if (req.altoMaxCm() != null && producto.getAltoMaxCm().compareTo(req.altoMaxCm()) != 0)
            cambios.add("Alto máximo: " + producto.getAltoMaxCm() + " → " + req.altoMaxCm() + " cm");
        Long telaAnterior = producto.getTelaDefecto() == null ? null : producto.getTelaDefecto().getId();
        Long mecanismoAnterior = producto.getMecanismoDefecto() == null ? null : producto.getMecanismoDefecto().getId();
        if (!Objects.equals(telaAnterior, req.telaDefectoId())) cambios.add("Tela predeterminada modificada");
        if (!Objects.equals(mecanismoAnterior, req.mecanismoDefectoId())) cambios.add("Mecanismo predeterminado modificado");
        producto.setNombre(req.nombre());
        producto.setDescripcion(req.descripcion());
        producto.setCategoria(req.categoria());
        producto.setImagenPrincipal(req.imagenPrincipal());
        if (req.anchoMaxCm() != null) producto.setAnchoMaxCm(req.anchoMaxCm());
        if (req.altoMaxCm() != null) producto.setAltoMaxCm(req.altoMaxCm());
        producto.setTelaDefecto(req.telaDefectoId() == null ? null : telaRepository.findById(req.telaDefectoId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "La tela indicada no existe.")));
        producto.setMecanismoDefecto(req.mecanismoDefectoId() == null ? null : mecanismoRepository.findById(req.mecanismoDefectoId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "El mecanismo indicado no existe.")));
        producto = productoRepository.save(producto);
        if (!cambios.isEmpty()) registrarCambio(actorId, "EDITAR_PRODUCTO", producto.getId(), String.join("; ", cambios));
        return ProductoResponse.desde(producto);
    }

    @Transactional
    public ProductoResponse actualizarEstado(Long id, boolean activo, Long actorId) {
        Producto producto = buscarOFallar(id);
        boolean cambio = !Objects.equals(producto.getActivo(), activo);
        producto.setActivo(activo);
        producto = productoRepository.save(producto);
        if (cambio) registrarCambio(actorId, activo ? "ACTIVAR_PRODUCTO" : "ARCHIVAR_PRODUCTO", id, "Producto: " + producto.getNombre());
        return ProductoResponse.desde(producto);
    }

    private void registrarCambio(Long actorId, String accion, Long recursoId, String detalle) {
        AuditoriaAdministrativa evento = new AuditoriaAdministrativa();
        evento.actorId = actorId;
        evento.accion = accion;
        evento.recurso = "PRODUCTO";
        evento.recursoId = recursoId;
        // La columna detalle admite 250 caracteres; evitamos fallos al guardar textos largos.
        evento.detalle = detalle.length() > 250 ? detalle.substring(0, 247) + "..." : detalle;
        auditoriaRepository.save(evento);
        AuditoriaRegistroHttp.marcarRegistrado();
    }

    private Producto buscarOFallar(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Producto no encontrado."));
    }
}
