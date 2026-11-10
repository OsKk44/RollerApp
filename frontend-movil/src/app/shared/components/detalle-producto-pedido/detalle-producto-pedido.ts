import { Component, inject, input, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { PedidoService } from '../../../core/services/pedido.service';
import { Cotizacion } from '../../../core/models/cotizacion.model';

@Component({
  selector: 'app-detalle-producto-pedido',
  imports: [DecimalPipe],
  templateUrl: './detalle-producto-pedido.html',
  styleUrl: './detalle-producto-pedido.scss',
})
export class DetalleProductoPedidoComponent {
  pedidoId = input.required<number>();
  private api = inject(PedidoService);
  abierto = signal(false);
  cargando = signal(false);
  error = signal(false);
  detalle = signal<Cotizacion | null>(null);

  alternar(): void {
    if (this.abierto()) { this.abierto.set(false); return; }
    this.abierto.set(true);
    if (this.detalle() || this.cargando()) return;
    this.cargar();
  }
  cargar(): void {
    if (this.cargando()) return;
    this.cargando.set(true);
    this.error.set(false);
    this.api.detalleProducto(this.pedidoId()).subscribe({
      next: detalle => { this.detalle.set(detalle); this.cargando.set(false); },
      error: () => { this.error.set(true); this.cargando.set(false); },
    });
  }
}
