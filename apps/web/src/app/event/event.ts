export interface Event {
  id: string;
  title: string;
  description: string | null;
  location: string;
  startsAt: string;
  capacity: number;
}
