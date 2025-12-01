# Invoice Generation System - User Guide

## Overview

This invoice generation system automatically creates Excel invoices matching your current format. It tracks work hours, calculates totals, and generates professional invoices for each technician.

## Features

✅ **Automatic Invoice Generation** - Creates Excel files matching your exact format  
✅ **Work Log Tracking** - Records date, time, location, and description for each job  
✅ **Automatic Calculations** - Hours worked and total amounts calculated automatically  
✅ **Weekly Invoicing** - Group work logs by week for easy invoicing  
✅ **Multiple Technicians** - Each technician can have their own invoices  
✅ **GST Support** - Optional GST calculation  
✅ **Bank Details** - Include BSB and account number on invoices  

---

## Workflow

### 1. Record Work Logs

As technicians complete jobs, create work logs:

```bash
POST /api/worklogs
```

**Request Body:**
```json
{
  "user": {"id": 1},
  "job": {"id": 5},
  "workDate": "2025-11-17",
  "startTime": "08:30:00",
  "endTime": "11:30:00",
  "hourlyRate": 35.00,
  "workDescription": "Battery, Inverter and conduit Installation and prewire",
  "jobAddress": "75 Guildford Prospect",
  "workType": "BATTERY_INSTALLATION"
}
```

**The system automatically calculates:**
- `hoursWorked` = 3.5 hours (from 8:30 to 11:30)
- `totalAmount` = $122.50 (3.5 × $35)

### 2. Generate Invoice

At the end of the week, generate an invoice for a technician:

```bash
POST /api/invoices/generate?technicianId=1&startDate=2025-11-17&endDate=2025-11-21
```

**Response:**
```json
{
  "id": 1,
  "invoiceNumber": "01",
  "technicianName": "Mduduzi Frederick Dube",
  "weekNumber": 47,
  "subtotal": 1207.50,
  "gstAmount": 0.00,
  "totalAmount": 1207.50,
  "status": "DRAFT"
}
```

### 3. Update Invoice Details

Add billing information and bank details:

```bash
PUT /api/invoices/1
```

**Request Body:**
```json
{
  "billToName": "Nelvin Electrical",
  "billToAddress": "Seaford Height, SA 5169",
  "billToPhone": "0450 120 602",
  "billToEmail": "admin@nelvinelectrical.com.au",
  "technicianABN": "70150908415",
  "technicianAddress": "17 Ruby Way, Mount Barker",
  "technicianEmail": "tungatadube@gmail.com",
  "technicianPhone": "408704216",
  "bsb": "067-872",
  "accountNumber": "12345678",
  "gstRate": 0.00
}
```

### 4. Generate Excel File

Create the Excel invoice:

```bash
POST /api/invoices/1/generate-excel
```

**Response:** `"invoices/INV-17-11-25-Week-47-Invoice-Mduduzi-Frederick-Dube.xlsx"`

### 5. Download Invoice

```bash
GET /api/invoices/1/download
```

Downloads the Excel file directly.

---

## Work Log Types

The system supports these work types:
- `BATTERY_INSTALLATION`
- `INVERTER_INSTALLATION`
- `CONDUIT_INSTALLATION`
- `PREWIRE`
- `PANEL_INSTALLATION`
- `ELECTRICAL_WORK`
- `WAREHOUSE_WORK`
- `INSPECTION`
- `MAINTENANCE`
- `OTHER`

---

## Example: Complete Weekly Flow

### Monday, November 17

**Job 1: Morning**
```bash
POST /api/worklogs
{
  "user": {"id": 1},
  "job": {"id": 101},
  "workDate": "2025-11-17",
  "startTime": "08:30:00",
  "endTime": "11:30:00",
  "hourlyRate": 35.00,
  "workDescription": "Battery, Inverter and conduit Installation and prewire",
  "jobAddress": "75 Guildford Prospect",
  "workType": "BATTERY_INSTALLATION"
}
```

**Job 2: Afternoon**
```bash
POST /api/worklogs
{
  "user": {"id": 1},
  "job": {"id": 102},
  "workDate": "2025-11-17",
  "startTime": "13:05:00",
  "endTime": "17:15:00",
  "hourlyRate": 35.00,
  "workDescription": "Battery Installation, Inverter and conduit installation",
  "jobAddress": "6 Crescent Road Modbury",
  "workType": "BATTERY_INSTALLATION"
}
```

**Job 3: Warehouse**
```bash
POST /api/worklogs
{
  "user": {"id": 1},
  "job": {"id": 103},
  "workDate": "2025-11-17",
  "startTime": "17:30:00",
  "endTime": "18:00:00",
  "hourlyRate": 35.00,
  "workDescription": "Warehouse Offloading",
  "jobAddress": "Warehouse",
  "workType": "WAREHOUSE_WORK"
}
```

### Continue for the rest of the week...

### Friday, November 21 - Generate Invoice

```bash
# 1. Generate invoice
POST /api/invoices/generate?technicianId=1&startDate=2025-11-17&endDate=2025-11-21

# 2. Update with billing info
PUT /api/invoices/1
{
  "billToName": "Nelvin Electrical",
  "billToAddress": "Seaford Height, SA 5169",
  "billToPhone": "0450 120 602",
  "billToEmail": "admin@nelvinelectrical.com.au",
  "technicianABN": "70150908415",
  "technicianAddress": "17 Ruby Way, Mount Barker",
  "bsb": "067-872"
}

# 3. Generate Excel
POST /api/invoices/1/generate-excel

# 4. Download
GET /api/invoices/1/download
```

---

## API Endpoints Reference

### Work Logs

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/worklogs` | Create work log |
| GET | `/api/worklogs` | Get all work logs |
| GET | `/api/worklogs/{id}` | Get work log by ID |
| GET | `/api/worklogs/user/{userId}` | Get user's work logs |
| GET | `/api/worklogs/user/{userId}/uninvoiced` | Get uninvoiced work |
| GET | `/api/worklogs/job/{jobId}` | Get job's work logs |
| PUT | `/api/worklogs/{id}` | Update work log |
| DELETE | `/api/worklogs/{id}` | Delete work log (only if not invoiced) |

### Invoices

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/invoices/generate` | Generate new invoice |
| POST | `/api/invoices/{id}/generate-excel` | Create Excel file |
| GET | `/api/invoices/{id}` | Get invoice details |
| GET | `/api/invoices/{id}/download` | Download Excel file |
| GET | `/api/invoices/technician/{id}` | Get technician's invoices |
| PUT | `/api/invoices/{id}` | Update invoice details |

---

## Database Schema

### work_logs Table
```sql
- id (Primary Key)
- user_id (Foreign Key → users)
- job_id (Foreign Key → jobs)
- work_date
- start_time
- end_time
- hours_worked (auto-calculated)
- hourly_rate
- total_amount (auto-calculated)
- work_description
- job_address
- work_type
- invoiced (boolean)
- invoice_id (Foreign Key → invoices, nullable)
```

### invoices Table
```sql
- id (Primary Key)
- invoice_number
- technician_id (Foreign Key → users)
- invoice_date
- period_start_date
- period_end_date
- week_number
- bill_to_name
- bill_to_address
- bill_to_phone
- bill_to_email
- technician_name
- technician_abn
- technician_address
- technician_email
- technician_phone
- bsb
- account_number
- subtotal (auto-calculated)
- gst_rate
- gst_amount (auto-calculated)
- total_amount (auto-calculated)
- status (DRAFT, SENT, PAID, OVERDUE, CANCELLED)
```

---

## Integration with Jobs

Work logs are linked to jobs, so you can:

1. **Track who worked on which job**
2. **See total labor cost per job**
3. **Generate job completion reports**

```bash
# Get all work done on a specific job
GET /api/worklogs/job/5

# See labor cost breakdown
GET /api/jobs/5/stock-cost  # Stock costs
GET /api/worklogs/job/5     # Labor costs
```

---

## Tips for Best Results

1. **Record work logs daily** - Don't wait until end of week
2. **Use consistent addresses** - Match job site addresses exactly
3. **Be specific in descriptions** - Include what was installed
4. **Always include time ranges** - System calculates hours automatically
5. **Review before generating** - Check all work logs are correct
6. **Update billing info once** - Save company details for reuse

---

## Excel Output Format

The generated Excel file matches your current format exactly:

```
Name: Mduduzi Frederick Dube          ABN: 70150908415
Address: 17 Ruby Way                  Email: tungatadube@gmail.com
Mount Barker                          Phone: 408704216

Bill To: Nelvin Electrical            Invoice #: 01
Address: Seaford Height, SA 5169      Week Number: 47

Invoice For: 17/11/25 - 21/11/25      Invoice Date: 17/11/25

Date                          Address                Description                                    Price
Monday 17 November 2025      75 Guildford Prospect  Battery, Inverter and conduit... (8:30-11:30) 122.50
                             6 Crescent Road        Battery Installation... (13:05-17:15)          140.00
...

                                                    Invoice Subtotal:    1207.50
                                                    GST (If Registered):    0.00
                                                    GST Amount:             0.00
Bank Details: Mduduzi Dube                         
BSB: 067-872                                        Deposit To Be Paid:  1207.50
```

---

## Next Steps

1. Copy these files to your backend
2. Run database migrations to create new tables
3. Test with sample work logs
4. Generate your first invoice!

Need help? Check the main README or contact support.
