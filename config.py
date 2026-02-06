import os
from dotenv import load_dotenv

load_dotenv()

# Database Configuration
DB_NAME = os.environ.get('DB_NAME')
DB_USER = os.environ.get('DB_USER')
DB_PASSWORD = os.environ.get('DB_PASSWORD')
DB_HOST = os.environ.get('DB_HOST')
DB_PORT = os.environ.get('DB_PORT')

# JWT Configuration
SECRET_KEY = os.environ.get('SECRET_KEY', default='uzbekiston-juda-xavfsiz-kalit-1234567890')
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 3000  # Access token - 50 hours
REFRESH_TOKEN_EXPIRE_DAYS = 15  # Refresh token - 15 days

# Email Configuration (Brevo)
BREVO_API_KEY = os.getenv("BREVO_API_KEY")
EMAIL_FROM = os.getenv("EMAIL_FROM")
SMTP_HOST = os.environ.get('SMTP_HOST')
SMTP_PORT = int(os.environ.get("SMTP_PORT", 587))
SMTP_USERNAME = os.environ.get('SMTP_USERNAME')
SMTP_PASSWORD = os.environ.get('SMTP_PASSWORD')

# Telegram Bot Configuration (for notifications)
TELEGRAM_BOT_TOKEN = os.environ.get('TELEGRAM_BOT_TOKEN')
