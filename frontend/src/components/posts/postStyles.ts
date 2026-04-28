export const postEditorialCss = `
.post-page { min-height: 100vh; background: #f6f4ef; padding: 56px 16px 96px; font-family: 'Inter', system-ui, sans-serif; color: #1a1a1a; }
.post-shell { max-width: 720px; margin: 0 auto; }
.post-eyebrow { font-size: 12px; letter-spacing: 0.18em; text-transform: uppercase; color: #8a7f6f; margin: 0 0 8px; }
.post-h1 { font-family: 'Source Serif Pro', Georgia, 'Times New Roman', serif; font-size: 34px; line-height: 1.15; margin: 0 0 28px; font-weight: 600; }
.post-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; gap: 12px; }
.post-toolbar-right { display: flex; gap: 10px; align-items: center; }
.post-btn { display: inline-block; padding: 10px 18px; background: #1a1a1a; color: #fff; border: 0; border-radius: 8px; font-size: 14px; font-weight: 600; letter-spacing: 0.02em; cursor: pointer; text-decoration: none; transition: background 120ms; }
.post-btn:hover:not(:disabled) { background: #000; }
.post-btn:disabled { background: #6e6358; cursor: not-allowed; }
.post-btn.is-ghost { background: #fff; color: #1a1a1a; border: 1px solid #ddd5c5; }
.post-btn.is-ghost:hover:not(:disabled) { background: #fafaf6; }
.post-btn.is-danger { background: #b6324a; }
.post-btn.is-danger:hover:not(:disabled) { background: #8c2237; }
.post-list { list-style: none; padding: 0; margin: 0; border-top: 1px solid #ece8df; }
.post-list-item { border-bottom: 1px solid #ece8df; padding: 18px 4px; }
.post-list-link { display: block; color: #1a1a1a; text-decoration: none; }
.post-list-link:hover .post-list-title { text-decoration: underline; }
.post-list-title { font-family: 'Source Serif Pro', Georgia, 'Times New Roman', serif; font-size: 20px; font-weight: 600; margin: 0 0 6px; }
.post-list-meta { font-size: 12.5px; color: #8a7f6f; }
.post-empty { padding: 64px 0; text-align: center; color: #8a7f6f; font-size: 15px; }
.post-pagination { display: flex; justify-content: center; align-items: center; gap: 12px; margin-top: 28px; }
.post-pagination .post-btn { padding: 8px 16px; }
.post-pagination-info { font-size: 13px; color: #5b5247; }
.post-detail-meta { color: #8a7f6f; font-size: 13.5px; margin: -16px 0 28px; border-bottom: 1px solid #ece8df; padding-bottom: 18px; }
.post-detail-body { font-family: 'Source Serif Pro', Georgia, 'Times New Roman', serif; font-size: 18px; line-height: 1.7; white-space: pre-wrap; word-break: break-word; }
.post-alert { background: #fdf3f4; border: 1px solid #f1ccd2; color: #8c2237; padding: 12px 14px; border-radius: 8px; font-size: 14px; margin-bottom: 18px; }
.post-form-card { width: 100%; background: #fff; border: 1px solid #ece8df; border-radius: 14px; padding: 32px 32px 28px; box-shadow: 0 4px 24px rgba(20,20,30,0.05); }
.post-textarea { width: 100%; box-sizing: border-box; padding: 12px 14px; border: 1px solid #ddd5c5; border-radius: 8px; font-size: 15px; background: #fafaf6; font-family: inherit; resize: vertical; min-height: 220px; line-height: 1.6; transition: border-color 120ms, box-shadow 120ms; outline: none; }
.post-textarea:focus { border-color: #1a1a1a; box-shadow: 0 0 0 3px rgba(26,26,26,0.08); background: #fff; }
.post-textarea.is-error { border-color: #b6324a; background: #fdf3f4; }
`;
