/**
 * Environment Configuration
 * Centralized configuration management for the application
 */

interface EnvironmentConfig {
  apiBaseUrl: string;
  apiChatEndpoint: string;
  apiDocumentsEndpoint: string;
  enableDebugMode: boolean;
  appName: string;
  maxFileSize: number;
  maxTextLength: number;
}

const getEnvironmentVariable = (key: string, defaultValue: string = ''): string => {
  return import.meta.env[`VITE_${key}`] || defaultValue;
};

const getEnvironmentNumber = (key: string, defaultValue: number = 0): number => {
  const value = import.meta.env[`VITE_${key}`];
  return value ? parseInt(value, 10) : defaultValue;
};

const getEnvironmentBoolean = (key: string, defaultValue: boolean = false): boolean => {
  const value = import.meta.env[`VITE_${key}`];
  return value ? value === 'true' : defaultValue;
};

const envConfig: EnvironmentConfig = {
  apiBaseUrl: getEnvironmentVariable('API_BASE_URL', ''),
  apiChatEndpoint: getEnvironmentVariable('API_CHAT_ENDPOINT', '/api/chat/send'),
  apiDocumentsEndpoint: getEnvironmentVariable('API_DOCUMENTS_ENDPOINT', '/api/documents/process'),
  enableDebugMode: getEnvironmentBoolean('ENABLE_DEBUG_MODE', false),
  appName: getEnvironmentVariable('APP_NAME', 'Zoho Recruit Sourcing Tool'),
  maxFileSize: getEnvironmentNumber('MAX_FILE_SIZE', 10485760),
  maxTextLength: getEnvironmentNumber('MAX_TEXT_LENGTH', 10000),
};

export const config = Object.freeze(envConfig);

export const getApiUrl = (endpoint: string): string => {
  return `${envConfig.apiBaseUrl}${endpoint}`;
};

export const debugLog = (message: string, data?: unknown): void => {
  if (envConfig.enableDebugMode) {
    console.log(`[${envConfig.appName}]`, message, data);
  }
};
