export interface CategorySummary {
  id: string;
  slug: string;
  name: string;
  iconUrl?: string;
  displayOrder: number;
}

export interface TopicSummary {
  id: string;
  slug: string;
  name: string;
  categorySlug: string;
  groupCount: number;
}
