import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { KeyValuePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { PermissionService } from '../../../core/services/permission.service';
import { PermissionResponse, PermissionScope } from '../../../core/models/permission.model';

@Component({
  selector: 'app-permission-list',
  imports: [
    KeyValuePipe, FormsModule,
    MatCardModule, MatTableModule, MatFormFieldModule, MatSelectModule, MatInputModule
  ],
  templateUrl: './permission-list.component.html'
})
export class PermissionListComponent implements OnInit {
  private readonly svc = inject(PermissionService);

  all = signal<PermissionResponse[]>([]);
  scopeFilter: PermissionScope | '' = '';
  search = '';
  readonly cols = ['capability', 'scope', 'description'];

  filtered = computed(() => {
    const s = this.search.toLowerCase();
    const scope = this.scopeFilter;
    return this.all().filter(p =>
      (!scope || p.scope === scope) &&
      (!s || p.capability.toLowerCase().includes(s) || p.scope.toLowerCase().includes(s))
    );
  });

  scopeCounts = computed(() => {
    const map: Record<string, number> = {};
    for (const p of this.all()) map[p.scope] = (map[p.scope] ?? 0) + 1;
    return map;
  });

  ngOnInit(): void {
    this.svc.list().subscribe(p => this.all.set(p));
  }

  scopePill(scope: PermissionScope): string {
    return scope === 'GRANTED' ? 'pill pill-ok'
         : scope === 'READ_ONLY' ? 'pill pill-muted'
         : scope === 'ASSIGNMENT_RESTRICTED' ? 'pill pill-warn'
         : 'pill pill-err';
  }

  onSearchInput(v: string): void { this.search = v; }
  onScopeChange(): void { /* computed tracks signals via field; nothing explicit needed */ }
}
