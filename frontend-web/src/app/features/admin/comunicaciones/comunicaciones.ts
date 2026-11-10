import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { environment } from '../../../../environments/environment';
interface CambioAdministrativo {id:number;actorId:number;accion:string;recurso:string;recursoId:number;detalle:string|null;creadoEn:string;}
interface EventoAuditoria { id:number; actorId:number|null; accion:string; metodo:string; recurso:string; estado:number; duracionMs:number; origen:string; creadoEn:string; }
interface PaginaAuditoria { content:EventoAuditoria[]; number:number; totalPages:number; totalElements:number; }
interface Correo{id:number;destinatario:string;asunto:string;estado:string;intentos:number;creadoEn:string;enviadoEn:string|null;}
interface Estado{smtpConfigurado:boolean;correoEmpresaConfigurado:boolean;webpayAmbiente:string;retornoHttps:boolean;}
@Component({selector:'app-comunicaciones',imports:[DatePipe],templateUrl:'./comunicaciones.html',styleUrl:'./comunicaciones.scss'})
export class ComunicacionesComponent implements OnInit{
 private http=inject(HttpClient);private url=environment.apiUrl+'/admin/comunicaciones';correos=signal<Correo[]>([]);estado=signal<Estado|null>(null);error=signal('');ocupado=signal(false);
 eventosVisibles(){return this.eventos();}
 cambiosVisibles(){return this.cambios();}
 correosVisibles(){return this.correos();}
 estadoVisible(){return this.estado();}
 totalEventosVisible(){return this.total();}
 totalCambiosVisible(){return this.totalCambios();}
 vista=signal<'auditoria'|'comunicaciones'>('auditoria');
 cambios=signal<CambioAdministrativo[]>([]);paginaCambios=signal(0);paginasCambios=signal(0);totalCambios=signal(0);cargandoCambios=signal(false);errorCambios=signal('');
 eventos=signal<EventoAuditoria[]>([]); pagina=signal(0); paginas=signal(0); total=signal(0);
 cargandoAuditoria=signal(false); errorAuditoria=signal('');
 filtroAccion=signal(''); filtroOrigen=signal(''); filtroActor=signal(''); filtroDesde=signal(''); filtroHasta=signal('');
 private solicitudAuditoria=0;
 filtrosActivos(){return !!(this.filtroAccion()||this.filtroOrigen()||this.filtroActor()||this.filtroDesde()||this.filtroHasta());}
 filtrosFechaValidos(){return !this.filtroDesde()||!this.filtroHasta()||this.filtroDesde()<=this.filtroHasta();}
 limpiarFiltros(){this.filtroAccion.set('');this.filtroOrigen.set('');this.filtroActor.set('');this.filtroDesde.set('');this.filtroHasta.set('');this.cargarAuditoria(0);}
 accionLegible(a:string){return ({CONSULTAR:'Consulta',CREAR_O_EJECUTAR:'Creación o ejecución',MODIFICAR:'Modificación',ELIMINAR:'Eliminación'} as Record<string,string>)[a] ?? this.humanizar(a);}
 origenLegible(o:string){return ({WEB:'Sitio web',MOVIL:'Aplicación móvil',DESCONOCIDO:'No identificado'} as Record<string,string>)[o] ?? this.humanizar(o);}
 humanizar(v:string){return (v||'No especificado').replace(/[_-]+/g,' ').toLowerCase().replace(/^./,c=>c.toUpperCase());}
 usuarioLegible(id:number|null){return id==null?'Sin usuario identificado':'Usuario #'+id;}
 recursoLegible(r:string){const nombres:Record<string,string>={auth:'Acceso y cuentas',login:'Inicio de sesión',admin:'Administración',auditoria:'Auditoría',eventos:'Eventos',productos:'Productos',producto:'Producto',pedidos:'Pedidos',pedido:'Pedido',pagos:'Pagos',webpay:'Webpay',usuarios:'Usuarios',usuario:'Usuario',catalogo:'Catálogo',servicios:'Servicios',servicio:'Servicio',incidencias:'Incidencias',cotizaciones:'Cotizaciones',telas:'Telas',carrito:'Carrito de compra',resenas:'Reseñas',agenda:'Agenda',clientes:'Clientes',comunicaciones:'Comunicaciones',facturacion:'Facturación'};const partes=r.split('?')[0].split('/').filter(x=>x&&x!=='api'&&!/^\d+$/.test(x)&&!/^\{.*\}$/.test(x));return partes.map(x=>nombres[x.toLowerCase()]??this.humanizar(x)).join(' › ')||'Recurso no identificado';}
 resultadoLegible(estado:number){if(estado>=200&&estado<300)return 'Solicitud atendida';if(estado===400||estado===422)return 'Datos no válidos';if(estado===401)return 'Sin autenticación';if(estado===403)return 'Acceso denegado';if(estado===404)return 'No encontrado';if(estado===409)return 'Conflicto';if(estado===429)return 'Demasiadas solicitudes';if(estado>=500)return 'Error del servidor';if(estado>=400)return 'Solicitud rechazada';if(estado>=300)return 'Redirección';return 'En proceso';}
 correoLegible(estado:string){return ({ENVIADO:'Enviado',FALLIDO:'Fallido',PENDIENTE:'Pendiente'} as Record<string,string>)[estado]??this.humanizar(estado);}
 
 seleccionarVista(v:'auditoria'|'comunicaciones'):void{this.vista.set(v); if(v==='auditoria') this.cargarAuditoria(0); else this.cargar();}
 cargarCambios(pagina:number):void{
   if(pagina<0 || this.cargandoCambios())return;
   this.cargandoCambios.set(true);this.errorCambios.set('');
   this.http.get<{content:CambioAdministrativo[];number:number;totalElements:number;totalPages:number}>(
      environment.apiUrl+'/admin/auditoria?pagina='+pagina+'&cantidad=20').subscribe({
       next:d=>{this.cambios.set(d.content);this.paginaCambios.set(d.number);this.paginasCambios.set(d.totalPages);this.totalCambios.set(d.totalElements);this.cargandoCambios.set(false);},
       error:()=>{this.errorCambios.set('No fue posible consultar los cambios administrativos.');this.cargandoCambios.set(false);}
      });
 }
 cargarAuditoria(pagina:number):void{
   if(pagina<0)return;
   if(!this.filtrosFechaValidos()){this.errorAuditoria.set('La fecha inicial no puede ser posterior a la fecha final.');return;}
   const solicitud=++this.solicitudAuditoria;
   this.cargandoAuditoria.set(true); this.errorAuditoria.set('');
   if(pagina===0)this.cargarCambios(0);
   let params=new URLSearchParams({pagina:String(pagina),cantidad:'30'});
   if(this.filtroAccion())params.set('accion',this.filtroAccion());
   if(this.filtroOrigen())params.set('origen',this.filtroOrigen());
   if(/^\d+$/.test(this.filtroActor()) && Number(this.filtroActor())>0)params.set('actor',this.filtroActor());
   if(this.filtroDesde())params.set('desde',this.filtroDesde());
   if(this.filtroHasta())params.set('hasta',this.filtroHasta());
   this.http.get<PaginaAuditoria>(environment.apiUrl+'/admin/auditoria/eventos?'+params).subscribe({
      next:d=>{if(solicitud!==this.solicitudAuditoria)return;this.eventos.set(d.content);this.pagina.set(d.number);this.paginas.set(d.totalPages);this.total.set(d.totalElements);this.cargandoAuditoria.set(false);},
      error:()=>{if(solicitud!==this.solicitudAuditoria)return;this.errorAuditoria.set('No fue posible consultar el historial de auditoría. Comprueba la conexión y vuelve a intentar.');this.cargandoAuditoria.set(false);}
   });
 }

 ngOnInit():void{this.cargarAuditoria(0);}
 cargar():void{this.error.set('');this.http.get<Estado>(this.url+'/estado').subscribe({next:e=>this.estado.set(e),error:()=>this.error.set('No se pudo consultar la configuración.')});this.http.get<Correo[]>(this.url).subscribe({next:c=>this.correos.set(c),error:()=>this.error.set('No se pudieron consultar los correos.')});}
 reintentar(id:number):void{if(this.ocupado())return;this.ocupado.set(true);this.http.post(this.url+'/'+id+'/reintentar',{}).subscribe({next:()=>{this.ocupado.set(false);this.cargar();},error:e=>{this.ocupado.set(false);this.error.set(e.error?.mensaje ?? e.error?.error ?? 'No se pudo reintentar el envío.');}});}
}
