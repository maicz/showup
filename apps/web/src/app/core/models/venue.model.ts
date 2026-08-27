export interface GeoPoint {
  latitude: number;
  longitude: number;
}

export interface Address {
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  region?: string;
  postalCode?: string;
  country?: string;
}

export interface VenueSummary {
  id: string;
  name: string;
  address: Address;
  location?: GeoPoint;
  notes?: string;
}

export interface CreateVenueRequest {
  name: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  region?: string;
  postalCode?: string;
  country: string;
  location?: GeoPoint;
  notes?: string;
}
