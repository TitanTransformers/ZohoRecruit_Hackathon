export interface SearchHistoryItem {
  id: string;
  query: string;
  type: 'text' | 'pdf';
  fileName?: string;
  timestamp: number;
  candidatesCount?: number;
}

const STORAGE_KEY = 'search_history';
const MAX_HISTORY = 10;

class SearchHistoryService {
  getHistory(): SearchHistoryItem[] {
    try {
      const data = localStorage.getItem(STORAGE_KEY);
      return data ? JSON.parse(data) : [];
    } catch {
      return [];
    }
  }

  addToHistory(item: Omit<SearchHistoryItem, 'id' | 'timestamp'>): SearchHistoryItem {
    const history = this.getHistory();
    const newItem: SearchHistoryItem = {
      ...item,
      id: Date.now().toString(),
      timestamp: Date.now(),
    };

    history.unshift(newItem);
    const trimmed = history.slice(0, MAX_HISTORY);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(trimmed));
    return newItem;
  }

  deleteFromHistory(id: string): void {
    const history = this.getHistory();
    const filtered = history.filter(item => item.id !== id);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(filtered));
  }

  clearHistory(): void {
    localStorage.removeItem(STORAGE_KEY);
  }

  updateCandidatesCount(id: string, count: number): void {
    const history = this.getHistory();
    const item = history.find(h => h.id === id);
    if (item) {
      item.candidatesCount = count;
      localStorage.setItem(STORAGE_KEY, JSON.stringify(history));
    }
  }
}

export const searchHistoryService = new SearchHistoryService();
