import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'paiseInr', standalone: true })
export class PaiseInrPipe implements PipeTransform {
  transform(paise: string | number | null | undefined): string {
    if (paise == null || paise === '') return '—';
    const n = typeof paise === 'string' ? BigInt(paise) : BigInt(Math.round(paise));
    const rupees = Number(n / 100n);
    const fraction = Number(n % 100n);
    return '₹ ' + rupees.toLocaleString('en-IN') + '.' + fraction.toString().padStart(2, '0');
  }
}
