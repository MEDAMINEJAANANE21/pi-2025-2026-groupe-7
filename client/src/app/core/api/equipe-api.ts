import { inject, Injectable } from '@angular/core';
import { z } from 'zod';
import { Chauffeur, ChauffeurRequest, ChauffeurSchema, Vehicule, VehiculeRequest, VehiculeSchema } from '../models/api';
import { ApiClient } from './api-client';

@Injectable({ providedIn: 'root' })
export class EquipeApi {
  private readonly api = inject(ApiClient);

  chauffeurs(): Promise<Chauffeur[]> {
    return this.api.get('/chauffeurs', z.array(ChauffeurSchema));
  }

  saveChauffeur(id: number | null, request: ChauffeurRequest): Promise<Chauffeur> {
    return id === null
      ? this.api.post('/chauffeurs', ChauffeurSchema, request)
      : this.api.put(`/chauffeurs/${id}`, ChauffeurSchema, request);
  }

  deleteChauffeur(id: number): Promise<void> {
    return this.api.delete(`/chauffeurs/${id}`);
  }

  vehicules(): Promise<Vehicule[]> {
    return this.api.get('/vehicules', z.array(VehiculeSchema));
  }

  saveVehicule(id: number | null, request: VehiculeRequest): Promise<Vehicule> {
    return id === null
      ? this.api.post('/vehicules', VehiculeSchema, request)
      : this.api.put(`/vehicules/${id}`, VehiculeSchema, request);
  }

  deleteVehicule(id: number): Promise<void> {
    return this.api.delete(`/vehicules/${id}`);
  }

  envoyerPhotoChauffeur(id: number, photo: File): Promise<Chauffeur> {
    return this.api.put(`/chauffeurs/${id}/photo`, ChauffeurSchema, formData(photo));
  }

  retirerPhotoChauffeur(id: number): Promise<Chauffeur> {
    return this.api.deleteFor(`/chauffeurs/${id}/photo`, ChauffeurSchema);
  }

  envoyerPhotoVehicule(id: number, photo: File): Promise<Vehicule> {
    return this.api.put(`/vehicules/${id}/photo`, VehiculeSchema, formData(photo));
  }

  retirerPhotoVehicule(id: number): Promise<Vehicule> {
    return this.api.deleteFor(`/vehicules/${id}/photo`, VehiculeSchema);
  }
}

function formData(photo: File): FormData {
  const body = new FormData();
  body.append('photo', photo, photo.name);
  return body;
}
