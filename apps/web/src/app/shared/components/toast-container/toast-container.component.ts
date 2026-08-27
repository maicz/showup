import { Component, inject } from '@angular/core';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-toast-container',
  standalone: true,
  template: `
    <div class="toast-wrapper">
      @for (toast of toastService.toasts(); track toast.id) {
        <div class="toast-item toast-{{ toast.type }}">
          <div class="toast-content">
            @if (toast.title) {
              <div class="toast-title">{{ toast.title }}</div>
            }
            <div class="toast-message">{{ toast.message }}</div>
          </div>
          <button class="toast-close" (click)="toastService.remove(toast.id)">&times;</button>
        </div>
      }
    </div>
  `,
  styles: [
    `
      .toast-wrapper {
        position: fixed;
        bottom: 24px;
        right: 24px;
        display: flex;
        flex-direction: column;
        gap: 10px;
        z-index: 9999;
        max-width: 380px;
        width: calc(100% - 48px);
      }

      .toast-item {
        display: flex;
        align-items: flex-start;
        justify-content: space-between;
        padding: 12px 16px;
        border-radius: 10px;
        background: #ffffff;
        box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -4px rgba(0, 0, 0, 0.05);
        border: 1px solid #e2e8f0;
        animation: slideUp 200ms ease-out;

        &.toast-success {
          border-left: 5px solid var(--color-success);
        }
        &.toast-error {
          border-left: 5px solid var(--color-danger);
        }
        &.toast-warning {
          border-left: 5px solid var(--color-warning);
        }
        &.toast-info {
          border-left: 5px solid var(--color-accent);
        }
      }

      .toast-content {
        flex: 1;
        margin-right: 8px;
      }

      .toast-title {
        font-weight: 700;
        font-size: 0.9rem;
        margin-bottom: 2px;
      }

      .toast-message {
        font-size: 0.85rem;
        color: var(--color-text-muted);
      }

      .toast-close {
        background: transparent;
        border: none;
        font-size: 1.25rem;
        cursor: pointer;
        color: #94a3b8;
        line-height: 1;
        padding: 0;

        &:hover {
          color: #334155;
        }
      }

      @keyframes slideUp {
        from {
          opacity: 0;
          transform: translateY(12px);
        }
        to {
          opacity: 1;
          transform: translateY(0);
        }
      }
    `,
  ],
})
export class ToastContainerComponent {
  readonly toastService = inject(ToastService);
}
