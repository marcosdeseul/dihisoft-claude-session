export const authEditorialCss = `
.auth-edit-page { min-height: 100vh; background: #f6f4ef; display: grid; place-items: center; padding: 48px 16px; font-family: 'Inter', system-ui, sans-serif; color: #1a1a1a; }
.auth-edit-card { width: 100%; max-width: 440px; background: #fff; border: 1px solid #ece8df; border-radius: 14px; padding: 40px 36px 32px; box-shadow: 0 4px 24px rgba(20,20,30,0.05); }
.auth-edit-eyebrow { font-size: 12px; letter-spacing: 0.18em; text-transform: uppercase; color: #8a7f6f; margin: 0 0 12px; }
.auth-edit-h1 { font-family: 'Source Serif Pro', Georgia, 'Times New Roman', serif; font-size: 30px; line-height: 1.15; margin: 0 0 8px; font-weight: 600; }
.auth-edit-sub { color: #5b5247; font-size: 15px; margin: 0 0 28px; }
.auth-edit-field { margin-bottom: 18px; }
.auth-edit-label { display: block; font-size: 13px; color: #4a4036; margin-bottom: 6px; font-weight: 500; }
.auth-edit-inputwrap { position: relative; }
.auth-edit-input { width: 100%; box-sizing: border-box; padding: 12px 36px 12px 14px; border: 1px solid #ddd5c5; border-radius: 8px; font-size: 15px; background: #fafaf6; transition: border-color 120ms, box-shadow 120ms; outline: none; }
.auth-edit-input:focus { border-color: #1a1a1a; box-shadow: 0 0 0 3px rgba(26,26,26,0.08); background: #fff; }
.auth-edit-input.is-error { border-color: #b6324a; background: #fdf3f4; }
.auth-edit-mark { position: absolute; right: 12px; top: 50%; transform: translateY(-50%); font-size: 14px; }
.auth-edit-mark.ok { color: #2c7a4d; }
.auth-edit-mark.bad { color: #b6324a; }
.auth-edit-fielderr { color: #b6324a; font-size: 12.5px; margin-top: 6px; }
.auth-edit-alert { background: #fdf3f4; border: 1px solid #f1ccd2; color: #8c2237; padding: 12px 14px; border-radius: 8px; font-size: 14px; margin-bottom: 18px; }
.auth-edit-success { background: #eef7f1; border: 1px solid #c8e3d2; color: #1f5b3a; padding: 12px 14px; border-radius: 8px; font-size: 14px; margin-bottom: 18px; }
.auth-edit-cta { width: 100%; padding: 13px 16px; background: #1a1a1a; color: #fff; border: 0; border-radius: 8px; font-size: 15px; font-weight: 600; letter-spacing: 0.02em; cursor: pointer; transition: background 120ms; }
.auth-edit-cta:hover:not(:disabled) { background: #000; }
.auth-edit-cta:disabled { background: #6e6358; cursor: not-allowed; }
.auth-edit-foot { margin-top: 18px; text-align: center; font-size: 13.5px; color: #5b5247; }
.auth-edit-foot a { color: #1a1a1a; font-weight: 500; text-decoration: none; }
.auth-edit-foot a:hover { text-decoration: underline; }
`;
