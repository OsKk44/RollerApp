import { DetalleProductoPedidoComponent } from '../../shared/components/detalle-producto-pedido/detalle-producto-pedido';
import { etiquetaEstado } from '../../core/utils/estado-pedido';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { fechaChile } from '../../core/utils/fecha-chile';
import { ObservacionesVisitaComponent } from '../../shared/components/observaciones-visita/observaciones-visita';
import { Component, OnInit, OnDestroy, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { PedidoService } from '../../core/services/pedido.service';
import { AgendaService } from '../../core/services/agenda.service';
import { ToastService } from '../../core/services/toast.service';
import { Pedido } from '../../core/models/pedido.model';

@Component({
  selector: 'app-mis-visitas',
  imports: [DetalleProductoPedidoComponent, FormsModule, ObservacionesVisitaComponent, DatePipe],
  templateUrl: './mis-visitas.html',
  styleUrl: './mis-visitas.scss',
})
export class MisVisitasComponent implements OnInit, OnDestroy {
  private pedidosApi = inject(PedidoService);
  private agendaApi = inject(AgendaService);
  private toast = inject(ToastService);
  etiquetaEstado=etiquetaEstado;
  fecha = fechaChile();
  private consulta?: Subscription;
  cambiarFecha(fecha: string): void {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(fecha)) return;
    this.fecha = fecha; this.cargar();
  }
  hoy(): void { this.cambiarFecha(fechaChile()); }
  ngOnDestroy(): void { this.consulta?.unsubscribe(); }
  busqueda = signal('');
  filtroEstado = signal('TODOS');
  visitasFiltradas = computed(() => {
    const q = this.busqueda().trim().toLocaleLowerCase('es-CL');
    return this.visitas().filter(v => {
      const texto = [v.id, v.visitaId, v.productoNombre, v.direccion, v.comuna].join(' ').toLocaleLowerCase('es-CL');
      return (!q || texto.includes(q)) && (this.filtroEstado() === 'TODOS' || v.visitaEstado === this.filtroEstado());
    });
  });
  limpiarFiltros(): void { this.busqueda.set(''); this.filtroEstado.set('TODOS'); }
  visitas = signal<Pedido[]>([]);
  cargando = signal(true);
  error = signal(false);
  procesando = signal<number | null>(null);

  ngOnInit(): void { this.cargar(); }

  iniciarAtencion(visita: Pedido): void {
    if (!visita.visitaId || visita.visitaEstado !== 'RESERVADO' || visita.atencionInicio || this.procesando() !== null) return;
    this.procesando.set(visita.visitaId);
    this.agendaApi.iniciar(visita.visitaId).subscribe({
      next: () => { this.procesando.set(null); this.cargar(); },
      error: e => { this.procesando.set(null); this.toast.mostrar(e.error?.mensaje ?? e.error?.error ?? 'No se pudo iniciar la atención.', 'error'); },
    });
  }

  cargar(): void {
    this.consulta?.unsubscribe();
    this.visitas.set([]);
    this.cargando.set(true);
    this.error.set(false);
    this.consulta = this.pedidosApi.listarMisVisitas(this.fecha).subscribe({
      next: visitas => { this.visitas.set(visitas); this.cargando.set(false); },
      error: () => { this.error.set(true); this.cargando.set(false); },
    });
  }

  completar(visita: Pedido): void {
    if (!visita.visitaId || visita.visitaEstado !== 'RESERVADO' || this.procesando() !== null) return;
    this.procesando.set(visita.visitaId);
    this.agendaApi.completar(visita.visitaId).subscribe({
      next: () => { this.procesando.set(null); this.toast.mostrar('Visita completada.', 'exito'); this.cargar(); },
      error: e => {
        this.procesando.set(null);
        this.toast.mostrar(e.error?.mensaje ?? e.error?.error ?? 'No se pudo completar la visita.', 'error');
      },
    });
  }
}
