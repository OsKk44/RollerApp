# Pruebas de auditoría real — RollerApp

## Alcance

La auditoría administrativa registra cambios relevantes de configuración y operaciones críticas, no la actividad de navegación. Cada evento tiene actor, fecha, acción, tipo de recurso e identificador disponible. Los productos y otros flujos que ya tienen instrumentación propia conservan sus detalles; el resto conserva la operación confirmada y el ID cuando aparece en la ruta. **No se promete un antes/después universal.**

La vista de seguridad registra intentos de inicio de sesión, errores de autorización (401/403) y solicitudes de escritura fallidas (4xx/5xx). No registra lecturas normales ni formularios ni credenciales. Los eventos antiguos anteriores a este cambio permanecen en el historial.

## Pruebas (en ambiente de prueba)

| Caso | Acción | Esperado |
|---|---|---|
| 01 | Editar nombre de producto desde web | Un solo cambio detallado con nombre anterior y nuevo |
| 02 | Repetir la edición sin modificar datos | Ningún nuevo cambio detallado de producto |
| 03 | Ver desde móvil el cambio hecho en web | Mismo registro, con mismo actor, fecha y recurso |
| 04 | Editar producto desde móvil | Registro visible en web |
| 05 | Crear, editar y archivar tela | Operaciones CREAR_TELA, ACTUALIZAR_TELA, ARCHIVAR_TELA |
| 06 | Crear, editar y archivar mecanismo | Operaciones CREAR_MECANISMO, ACTUALIZAR_MECANISMO, ARCHIVAR_MECANISMO |
| 07 | Crear o cancelar un bloque administrativo | Registro de agenda |
| 08 | Cambiar asignación de técnico | Registro detallado existente, sin duplicados |
| 09 | Cambiar rol o estado de usuario | Registro de usuario por actor con permisos |
| 10 | Modificar configuración de campos de facturación | Registro de configuración, sin valores fiscales del cliente |
| 11 | Liquidar comisión | Registro de operación financiera crítica |
| 12 | Reintentar envío administrativo | Registro de operación, sin contenido del correo |
| 13 | Acceso válido o fallido | Evento de seguridad, sin contraseña ni token |
| 14 | Solicitud sin permiso (403) | Evento de seguridad; ningún cambio administrativo |
| 15 | Escritura con validación fallida (400) | Solicitud fallida, ningún cambio administrativo |
| 16 | Navegar fichas, listas y paneles | Ningún registro nuevo de navegación |
| 17 | Iniciar pago del cliente | No se crea asiento administrativo |
| 18 | Repetir con cuentas admin distintas | Actor identificable correctamente |

## Límites y validación pendiente

Esta entrega incluye pruebas de clasificación de rutas ejecutadas con Java. Para certificar que la auditoría opera entre web y móvil falta ejecutar pruebas integrales con backend y base de datos real. El origen WEB/MOVIL proviene de una cabecera declarada por el cliente: no constituye verificación fuerte del dispositivo. Las operaciones críticas que no pasan por estos controladores requieren instrumentación explícita en sus servicios. Las tablas y eventos preexistentes no se borran ni migran.
