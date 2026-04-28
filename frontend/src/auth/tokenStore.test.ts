import { afterEach, describe, expect, it } from 'vitest';
import { clear, get, set } from './tokenStore';

afterEach(() => {
  clear();
});

describe('tokenStore', () => {
  it('초기_get은_null이다', () => {
    expect(get()).toBeNull();
  });

  it('set_후_get은_저장된_토큰을_반환한다', () => {
    set('abc.def.ghi');
    expect(get()).toBe('abc.def.ghi');
  });

  it('clear_후_get은_null이다', () => {
    set('abc.def.ghi');
    clear();
    expect(get()).toBeNull();
  });

  it('set이_연속_호출되면_마지막_값으로_덮어쓴다', () => {
    set('first');
    set('second');
    expect(get()).toBe('second');
  });
});
