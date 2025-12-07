// frontend/src/pages/Reports.tsx

import React, { useState, useEffect } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  FormControl,
  Grid,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Tab,
  Tabs,
  TextField,
  Typography,
} from '@mui/material';
import {
  Download as DownloadIcon,
  Receipt as ReceiptIcon,
  Assessment as AssessmentIcon,
  Work as WorkIcon,
  CalendarToday as CalendarIcon,
} from '@mui/icons-material';
import { DataGrid, GridColDef } from '@mui/x-data-grid';
import { invoiceApi, workLogApi, userApi, jobApi } from '../services/api';
import { Invoice, WorkLog, User, InvoiceStatus } from '../types';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

const TabPanel: React.FC<TabPanelProps> = ({ children, value, index }) => {
  return (
    <div hidden={value !== index}>
      {value === index && <Box sx={{ py: 3 }}>{children}</Box>}
    </div>
  );
};

const Reports: React.FC = () => {
  const [tabValue, setTabValue] = useState(0);
  const [users, setUsers] = useState<User[]>([]);
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [workLogs, setWorkLogs] = useState<WorkLog[]>([]);
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
  const [openInvoiceDialog, setOpenInvoiceDialog] = useState(false);
  const [invoiceForm, setInvoiceForm] = useState({
    startDate: '',
    endDate: '',
    billToName: 'Nelvin Electrical',
    billToAddress: 'Seaford Height, SA 5169',
    billToPhone: '0450 120 602',
    billToEmail: 'admin@nelvinelectrical.com.au',
  });

  useEffect(() => {
    loadUsers();
  }, []);

  useEffect(() => {
    if (selectedUserId) {
      loadInvoices();
      loadUninvoicedWork();
    }
  }, [selectedUserId]);

  const loadUsers = async () => {
    try {
      const response = await userApi.getAll();
      setUsers(response.data);
      if (response.data.length > 0) {
        setSelectedUserId(response.data[0].id);
      }
    } catch (error) {
      console.error('Failed to load users:', error);
    }
  };

  const loadInvoices = async () => {
    if (!selectedUserId) return;
    try {
      const response = await invoiceApi.getByTechnician(selectedUserId);
      setInvoices(response.data);
    } catch (error) {
      console.error('Failed to load invoices:', error);
    }
  };

  const loadUninvoicedWork = async () => {
    if (!selectedUserId) return;
    try {
      const response = await workLogApi.getUninvoiced(selectedUserId);
      setWorkLogs(response.data);
    } catch (error) {
      console.error('Failed to load uninvoiced work:', error);
    }
  };

  const handleGenerateInvoice = async () => {
    if (!selectedUserId || !invoiceForm.startDate || !invoiceForm.endDate) {
      alert('Please fill in all required fields');
      return;
    }

    try {
      // Step 1: Generate invoice
      const invoiceResponse = await invoiceApi.generate(
        selectedUserId,
        invoiceForm.startDate,
        invoiceForm.endDate
      );
      
      const invoice = invoiceResponse.data;

      // Step 2: Update with billing details
      await invoiceApi.update(invoice.id, {
        billToName: invoiceForm.billToName,
        billToAddress: invoiceForm.billToAddress,
        billToPhone: invoiceForm.billToPhone,
        billToEmail: invoiceForm.billToEmail,
      });

      // Step 3: Generate Excel file
      await invoiceApi.generateExcel(invoice.id);

      alert('Invoice generated successfully!');
      setOpenInvoiceDialog(false);
      loadInvoices();
      loadUninvoicedWork();
    } catch (error: any) {
      console.error('Failed to generate invoice:', error);
      alert(error.response?.data?.message || 'Failed to generate invoice');
    }
  };

  const handleDownloadInvoice = async (invoiceId: number) => {
    try {
      const response = await invoiceApi.download(invoiceId);
      const blob = new Blob([response.data], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `invoice-${invoiceId}.xlsx`;
      link.click();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error('Failed to download invoice:', error);
    }
  };

  const handleGenerateWeeklyInvoices = async () => {
    if (!window.confirm('Generate invoices for all technicians for the current week (Monday-Friday)?')) {
      return;
    }

    try {
      const response = await invoiceApi.generateWeekly();
      const generatedInvoices = response.data;

      alert(`Successfully generated ${generatedInvoices.length} invoice(s) for the current week.`);
      loadInvoices();
      loadUninvoicedWork();
    } catch (error: any) {
      console.error('Failed to generate weekly invoices:', error);
      alert(error.response?.data?.message || 'Failed to generate weekly invoices');
    }
  };

  const getStatusColor = (status: InvoiceStatus): "default" | "primary" | "secondary" | "success" | "warning" | "info" | "error" => {
    switch (status) {
      case InvoiceStatus.DRAFT: return 'default';
      case InvoiceStatus.SENT: return 'info';
      case InvoiceStatus.PAID: return 'success';
      case InvoiceStatus.OVERDUE: return 'error';
      case InvoiceStatus.CANCELLED: return 'default';
      default: return 'default';
    }
  };

  const invoiceColumns: GridColDef[] = [
    { field: 'invoiceNumber', headerName: 'Invoice #', width: 100 },
    {
      field: 'weekNumber',
      headerName: 'Week',
      width: 80,
    },
    {
      field: 'periodStartDate',
      headerName: 'Period',
      width: 200,
      valueGetter: (params) =>
        `${params.row.periodStartDate} - ${params.row.periodEndDate}`,
    },
    {
      field: 'totalAmount',
      headerName: 'Total',
      width: 120,
      valueFormatter: (params) => `$${params.value.toFixed(2)}`,
    },
    {
      field: 'status',
      headerName: 'Status',
      width: 120,
      renderCell: (params) => (
        <Chip label={params.value} color={getStatusColor(params.value)} size="small" />
      ),
    },
    {
      field: 'invoiceDate',
      headerName: 'Date',
      width: 120,
    },
    {
      field: 'actions',
      headerName: 'Actions',
      width: 100,
      sortable: false,
      renderCell: (params) => (
        <Button
          size="small"
          startIcon={<DownloadIcon />}
          onClick={() => handleDownloadInvoice(params.row.id)}
        >
          Download
        </Button>
      ),
    },
  ];

  const workLogColumns: GridColDef[] = [
    { field: 'workDate', headerName: 'Date', width: 120 },
    { field: 'jobAddress', headerName: 'Address', width: 200 },
    { field: 'workDescription', headerName: 'Description', width: 250 },
    {
      field: 'hoursWorked',
      headerName: 'Hours',
      width: 80,
      valueFormatter: (params) => params.value.toFixed(2),
    },
    {
      field: 'totalAmount',
      headerName: 'Amount',
      width: 100,
      valueFormatter: (params) => `$${params.value.toFixed(2)}`,
    },
  ];

  const totalUninvoiced = workLogs.reduce((sum, log) => sum + log.totalAmount, 0);

  return (
    <Box p={3}>
      <Typography variant="h4" gutterBottom>
        Reports & Invoicing
      </Typography>

      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }}>
        <Tabs value={tabValue} onChange={(e, newValue) => setTabValue(newValue)}>
          <Tab icon={<ReceiptIcon />} label="Invoices" />
          <Tab icon={<WorkIcon />} label="Uninvoiced Work" />
          <Tab icon={<AssessmentIcon />} label="Summary" />
        </Tabs>
      </Box>

      {/* User Selection */}
      <Box mb={3}>
        <FormControl sx={{ minWidth: 300 }}>
          <InputLabel>Select Technician</InputLabel>
          <Select
            value={selectedUserId || ''}
            onChange={(e) => setSelectedUserId(e.target.value as number)}
          >
            {users.map((user) => (
              <MenuItem key={user.id} value={user.id}>
                {user.firstName} {user.lastName}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      </Box>

      {/* Invoices Tab */}
      <TabPanel value={tabValue} index={0}>
        <Box display="flex" justifyContent="space-between" mb={2}>
          <Typography variant="h6">Generated Invoices</Typography>
          <Box>
            <Button
              variant="outlined"
              startIcon={<CalendarIcon />}
              onClick={handleGenerateWeeklyInvoices}
              sx={{ mr: 1 }}
            >
              Generate Weekly Invoices
            </Button>
            <Button
              variant="contained"
              startIcon={<ReceiptIcon />}
              onClick={() => setOpenInvoiceDialog(true)}
              disabled={!selectedUserId}
            >
              Generate New Invoice
            </Button>
          </Box>
        </Box>
        <Paper>
          <DataGrid
            rows={invoices}
            columns={invoiceColumns}
            pageSizeOptions={[10, 25, 50]}
            initialState={{
              pagination: { paginationModel: { pageSize: 10 } },
            }}
            autoHeight
          />
        </Paper>
      </TabPanel>

      {/* Uninvoiced Work Tab */}
      <TabPanel value={tabValue} index={1}>
        <Card sx={{ mb: 2 }}>
          <CardContent>
            <Typography variant="h6">Uninvoiced Work Summary</Typography>
            <Typography variant="h4" color="primary">
              ${totalUninvoiced.toFixed(2)}
            </Typography>
            <Typography variant="body2" color="textSecondary">
              {workLogs.length} work log(s)
            </Typography>
          </CardContent>
        </Card>
        <Paper>
          <DataGrid
            rows={workLogs}
            columns={workLogColumns}
            pageSizeOptions={[10, 25, 50]}
            initialState={{
              pagination: { paginationModel: { pageSize: 25 } },
            }}
            autoHeight
          />
        </Paper>
      </TabPanel>

      {/* Summary Tab */}
      <TabPanel value={tabValue} index={2}>
        <Grid container spacing={3}>
          <Grid item xs={12} md={4}>
            <Card>
              <CardContent>
                <Typography variant="subtitle2" color="textSecondary">
                  Total Invoices
                </Typography>
                <Typography variant="h4">{invoices.length}</Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} md={4}>
            <Card>
              <CardContent>
                <Typography variant="subtitle2" color="textSecondary">
                  Total Invoiced
                </Typography>
                <Typography variant="h4">
                  ${invoices.reduce((sum, inv) => sum + inv.totalAmount, 0).toFixed(2)}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} md={4}>
            <Card>
              <CardContent>
                <Typography variant="subtitle2" color="textSecondary">
                  Pending Payment
                </Typography>
                <Typography variant="h4">
                  $
                  {invoices
                    .filter((inv) => inv.status === InvoiceStatus.SENT)
                    .reduce((sum, inv) => sum + inv.totalAmount, 0)
                    .toFixed(2)}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </TabPanel>

      {/* Generate Invoice Dialog */}
      <Dialog
        open={openInvoiceDialog}
        onClose={() => setOpenInvoiceDialog(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Generate Invoice</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Start Date"
                type="date"
                fullWidth
                InputLabelProps={{ shrink: true }}
                value={invoiceForm.startDate}
                onChange={(e) => setInvoiceForm({ ...invoiceForm, startDate: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="End Date"
                type="date"
                fullWidth
                InputLabelProps={{ shrink: true }}
                value={invoiceForm.endDate}
                onChange={(e) => setInvoiceForm({ ...invoiceForm, endDate: e.target.value })}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                label="Bill To Name"
                fullWidth
                value={invoiceForm.billToName}
                onChange={(e) => setInvoiceForm({ ...invoiceForm, billToName: e.target.value })}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                label="Bill To Address"
                fullWidth
                value={invoiceForm.billToAddress}
                onChange={(e) => setInvoiceForm({ ...invoiceForm, billToAddress: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Bill To Phone"
                fullWidth
                value={invoiceForm.billToPhone}
                onChange={(e) => setInvoiceForm({ ...invoiceForm, billToPhone: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Bill To Email"
                fullWidth
                value={invoiceForm.billToEmail}
                onChange={(e) => setInvoiceForm({ ...invoiceForm, billToEmail: e.target.value })}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenInvoiceDialog(false)}>Cancel</Button>
          <Button onClick={handleGenerateInvoice} variant="contained">
            Generate Invoice
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Reports;
