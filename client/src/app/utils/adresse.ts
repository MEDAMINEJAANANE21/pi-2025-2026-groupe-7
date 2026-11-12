import { Adresse } from '../models/adresse';
import { AdresseDto } from '../models/backend';

export function toAdresse(dto: AdresseDto): Adresse {
  return {
    coords: dto.coordonnees,
    name: `${dto.numero} ${dto.rue}`.trim(),
    postCode: dto.codePostal,
    city: dto.ville,
  };
}
