import animate from 'tailwindcss-animate'

/** @type {import('tailwindcss').Config} */
export default {
  darkMode: ['class'],
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        ink: '#0f1115',
        steel: '#1a1d24',
        mist: '#f5f7fb',
        line: '#e8ecf2',
        soft: '#6b7280',
        accent: '#0ea5e9',
        success: '#16a34a',
        warn: '#d97706',
        danger: '#dc2626'
      },
      borderRadius: {
        lg: '8px',
        xl: '12px',
        '2xl': '16px'
      },
      boxShadow: {
        card: '0 6px 18px rgba(15,17,21,0.06)',
        hover: '0 10px 24px rgba(15,17,21,0.08)'
      },
      keyframes: {
        fadeUp: {
          from: { opacity: '0', transform: 'translateY(16px)' },
          to: { opacity: '1', transform: 'translateY(0)' }
        }
      },
      animation: {
        fadeUp: 'fadeUp .45s ease both'
      }
    }
  },
  plugins: [animate]
}
