let token: string | null = null;

export function get(): string | null {
  return token;
}

export function set(value: string): void {
  token = value;
}

export function clear(): void {
  token = null;
}
