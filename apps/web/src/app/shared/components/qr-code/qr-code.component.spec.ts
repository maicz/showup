import { ComponentFixture, TestBed } from '@angular/core/testing';
import { QrCodeComponent } from './qr-code.component';

describe('QrCodeComponent', () => {
  let component: QrCodeComponent;
  let fixture: ComponentFixture<QrCodeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [QrCodeComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(QrCodeComponent);
    component = fixture.componentInstance;
  });

  it('should create and generate deterministic matrix from code', () => {
    fixture.componentRef.setInput('value', 'ticket_test_code_12345');
    fixture.componentRef.setInput('size', 200);
    fixture.detectChanges();

    expect(component).toBeTruthy();
    const grid = component.grid();
    expect(grid.length).toBe(25);
    expect(grid[0].length).toBe(25);

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('svg')).toBeTruthy();
  });
});
