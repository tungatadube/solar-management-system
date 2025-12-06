/**
 * Formats a Date object to ISO 8601 string in local timezone (without Z suffix)
 * This ensures dates are sent to the backend without timezone conversion
 *
 * Example: Date("2025-12-06 10:00:00 Adelaide") => "2025-12-06T10:00:00"
 */
export const formatDateToLocalISO = (date: Date | null): string | undefined => {
  if (!date) return undefined;

  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  const seconds = String(date.getSeconds()).padStart(2, '0');

  return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`;
};
