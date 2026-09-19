import { DOCUMENT } from '@angular/common';
import { AfterViewInit, Directive, ElementRef, HostListener, OnDestroy, inject } from '@angular/core';

const FOCUSABLE_SELECTOR = [
  'a[href]',
  'button:not([disabled])',
  'input:not([disabled])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  '[tabindex]:not([tabindex="-1"])',
].join(',');

/**
 * Keeps keyboard focus inside a transient dialog and returns it to the invoking control when the
 * dialog is removed. Dialogs are conditionally rendered, so remembering the active element during
 * initialization naturally captures the trigger before the overlay takes focus.
 */
@Directive({
  selector: '[appFocusTrap]',
  standalone: true,
})
export class FocusTrapDirective implements AfterViewInit, OnDestroy {
  private readonly host = inject(ElementRef<HTMLElement>);
  private readonly document = inject(DOCUMENT);
  private readonly previousFocus = this.document.activeElement instanceof HTMLElement ? this.document.activeElement : null;

  ngAfterViewInit() {
    queueMicrotask(() => this.focusableElements()[0]?.focus());
  }

  @HostListener('keydown.tab', ['$event'])
  trapTab(event: KeyboardEvent) {
    const focusable = this.focusableElements();
    if (focusable.length === 0) {
      event.preventDefault();
      this.host.nativeElement.focus();
      return;
    }

    const first = focusable[0];
    const last = focusable[focusable.length - 1];
    const active = this.document.activeElement;
    if (event.shiftKey && (active === first || !this.host.nativeElement.contains(active))) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && (active === last || !this.host.nativeElement.contains(active))) {
      event.preventDefault();
      first.focus();
    }
  }

  ngOnDestroy() {
    queueMicrotask(() => this.previousFocus?.focus());
  }

  private focusableElements(): HTMLElement[] {
    const elements = this.host.nativeElement.querySelectorAll(FOCUSABLE_SELECTOR) as NodeListOf<HTMLElement>;
    return Array.from(elements)
      .filter(element => !element.hasAttribute('hidden') && element.offsetParent !== null);
  }
}
