import { Injectable, signal } from '@angular/core';

export interface Toast {
  id: string;
  type: 'success' | 'error' | 'info' | 'warning';
  title?: string;
  message: string;
}

@Injectable({
  providedIn: 'root',
})
export class ToastService {
  private readonly _toasts = signal<Toast[]>([]);
  readonly toasts = this._toasts.asReadonly();

  show(type: 'success' | 'error' | 'info' | 'warning', message: string, title?: string, durationMs = 4000) {
    const id = Math.random().toString(36).substring(2, 9);
    const toast: Toast = { id, type, message, title };
    this._toasts.update(t => [...t, toast]);

    if (durationMs > 0) {
      setTimeout(() => this.remove(id), durationMs);
    }
  }

  success(message: string, title = 'Success') {
    this.show('success', message, title);
  }

  error(message: string, title = 'Error') {
    this.show('error', message, title, 6000);
  }

  info(message: string, title = 'Notice') {
    this.show('info', message, title);
  }

  warning(message: string, title = 'Warning') {
    this.show('warning', message, title);
  }

  remove(id: string) {
    this._toasts.update(t => t.filter(item => item.id !== id));
  }
}
