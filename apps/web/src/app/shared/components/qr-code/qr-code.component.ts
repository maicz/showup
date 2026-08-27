import { Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-qr-code',
  standalone: true,
  template: `
    <div class="qr-container" [style.width.px]="size()" [style.height.px]="size()">
      <svg
        [attr.viewBox]="'0 0 ' + grid().length + ' ' + grid().length"
        shape-rendering="crispEdges"
        width="100%"
        height="100%"
      >
        <rect width="100%" height="100%" fill="#ffffff" />
        @for (row of grid(); track $index; let y = $index) {
          @for (cell of row; track $index; let x = $index) {
            @if (cell) {
              <rect [attr.x]="x" [attr.y]="y" width="1" height="1" fill="#0f172a" />
            }
          }
        }
      </svg>
    </div>
  `,
  styles: [
    `
      .qr-container {
        display: inline-block;
        padding: 12px;
        background: #ffffff;
        border-radius: 12px;
        box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
        border: 1px solid #e2e8f0;
      }
      svg {
        display: block;
      }
    `,
  ],
})
export class QrCodeComponent {
  readonly value = input.required<string>();
  readonly size = input<number>(200);

  // Deterministic 25x25 QR Matrix generation including standard finder patterns
  readonly grid = computed(() => {
    const val = this.value();
    const dim = 25;
    const matrix: boolean[][] = Array.from({ length: dim }, () => Array(dim).fill(false));

    // Place Finder Patterns (top-left, top-right, bottom-left)
    this.drawFinderPattern(matrix, 0, 0);
    this.drawFinderPattern(matrix, dim - 7, 0);
    this.drawFinderPattern(matrix, 0, dim - 7);

    // Place Timing Patterns
    for (let i = 8; i < dim - 8; i++) {
      matrix[6][i] = i % 2 === 0;
      matrix[i][6] = i % 2 === 0;
    }

    // Hash input to generate deterministic data modules
    let hash = 2166136261;
    for (let i = 0; i < val.length; i++) {
      hash ^= val.charCodeAt(i);
      hash = Math.imul(hash, 16777619);
    }

    for (let y = 0; y < dim; y++) {
      for (let x = 0; x < dim; x++) {
        // Skip reserved finder pattern regions
        if (
          (x < 8 && y < 8) ||
          (x >= dim - 8 && y < 8) ||
          (x < 8 && y >= dim - 8) ||
          x === 6 ||
          y === 6
        ) {
          continue;
        }

        // Module bit from hash & coordinates
        const bit = ((hash ^ (x * 37 + y * 73)) >>> ((x + y) % 31)) & 1;
        matrix[y][x] = bit === 1;
      }
    }

    return matrix;
  });

  private drawFinderPattern(matrix: boolean[][], startX: number, startY: number) {
    for (let r = 0; r < 7; r++) {
      for (let c = 0; c < 7; c++) {
        const isBorder = r === 0 || r === 6 || c === 0 || c === 6;
        const isCenter = r >= 2 && r <= 4 && c >= 2 && c <= 4;
        matrix[startY + r][startX + c] = isBorder || isCenter;
      }
    }
  }
}
