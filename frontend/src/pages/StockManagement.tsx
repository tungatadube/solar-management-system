// frontend/src/pages/StockManagement.tsx

import React, { useEffect, useState } from 'react';
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
  Grid,
  IconButton,
  Paper,
  TextField,
  Typography,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Warning as WarningIcon,
} from '@mui/icons-material';
import { DataGrid, GridColDef } from '@mui/x-data-grid';
import { stockApi } from '../services/api';
import { StockItem, StockCategory } from '../types';

const StockManagement: React.FC = () => {
  const [stockItems, setStockItems] = useState<StockItem[]>([]);
  const [lowStockItems, setLowStockItems] = useState<StockItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [openDialog, setOpenDialog] = useState(false);
  const [editingItem, setEditingItem] = useState<StockItem | null>(null);
  const [formData, setFormData] = useState<Partial<StockItem>>({
    category: StockCategory.OTHER,
    unit: 'piece',
    minimumQuantity: 10,
    reorderLevel: 20,
  });

  useEffect(() => {
    loadStockItems();
    loadLowStock();
  }, []);

  const loadStockItems = async () => {
    try {
      setLoading(true);
      const response = await stockApi.getAll();
      setStockItems(response.data);
    } catch (error) {
      console.error('Failed to load stock items:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadLowStock = async () => {
    try {
      const response = await stockApi.getLowStock();
      setLowStockItems(response.data);
    } catch (error) {
      console.error('Failed to load low stock:', error);
    }
  };

  const handleSave = async () => {
    try {
      if (editingItem) {
        await stockApi.update(editingItem.id, formData);
      } else {
        await stockApi.create(formData);
      }
      setOpenDialog(false);
      setEditingItem(null);
      setFormData({
        category: StockCategory.OTHER,
        unit: 'piece',
        minimumQuantity: 10,
        reorderLevel: 20,
      });
      loadStockItems();
      loadLowStock();
    } catch (error) {
      console.error('Failed to save stock item:', error);
    }
  };

  const handleEdit = (item: StockItem) => {
    setEditingItem(item);
    setFormData(item);
    setOpenDialog(true);
  };

  const handleDelete = async (id: number) => {
    if (window.confirm('Are you sure you want to delete this item?')) {
      try {
        await stockApi.delete(id);
        loadStockItems();
        loadLowStock();
      } catch (error) {
        console.error('Failed to delete stock item:', error);
      }
    }
  };

  const columns: GridColDef[] = [
    { field: 'sku', headerName: 'SKU', width: 120 },
    { field: 'name', headerName: 'Name', width: 200 },
    { field: 'category', headerName: 'Category', width: 150 },
    {
      field: 'totalQuantity',
      headerName: 'Quantity',
      width: 100,
      renderCell: (params) => (
        <Chip
          label={params.value || 0}
          color={
            params.value && params.row.minimumQuantity && params.value <= params.row.minimumQuantity
              ? 'error'
              : 'success'
          }
          size="small"
        />
      ),
    },
    { field: 'unit', headerName: 'Unit', width: 80 },
    {
      field: 'unitPrice',
      headerName: 'Unit Price',
      width: 100,
      valueFormatter: (params) => `$${params.value.toFixed(2)}`,
    },
    { field: 'minimumQuantity', headerName: 'Min Qty', width: 80 },
    { field: 'reorderLevel', headerName: 'Reorder', width: 80 },
    {
      field: 'actions',
      headerName: 'Actions',
      width: 120,
      sortable: false,
      renderCell: (params) => (
        <Box>
          <IconButton size="small" onClick={() => handleEdit(params.row)}>
            <EditIcon />
          </IconButton>
          <IconButton size="small" onClick={() => handleDelete(params.row.id)}>
            <DeleteIcon />
          </IconButton>
        </Box>
      ),
    },
  ];

  return (
    <Box p={3}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Stock Management</Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => {
            setEditingItem(null);
            setFormData({
              category: StockCategory.OTHER,
              unit: 'piece',
              minimumQuantity: 10,
              reorderLevel: 20,
            });
            setOpenDialog(true);
          }}
        >
          Add Stock Item
        </Button>
      </Box>

      {/* Low Stock Alert */}
      {lowStockItems.length > 0 && (
        <Card sx={{ mb: 3, backgroundColor: '#fff3e0' }}>
          <CardContent>
            <Box display="flex" alignItems="center" mb={2}>
              <WarningIcon color="warning" sx={{ mr: 1 }} />
              <Typography variant="h6">Low Stock Alert - {lowStockItems.length} Items</Typography>
            </Box>
            <Grid container spacing={2}>
              {lowStockItems.map((item) => (
                <Grid item xs={12} sm={6} md={4} key={item.id}>
                  <Paper sx={{ p: 2 }}>
                    <Typography variant="subtitle2">{item.name}</Typography>
                    <Typography variant="caption" color="textSecondary">
                      SKU: {item.sku} | Qty: {item.totalQuantity || 0} {item.unit}
                    </Typography>
                  </Paper>
                </Grid>
              ))}
            </Grid>
          </CardContent>
        </Card>
      )}

      {/* Stock Items Table */}
      <Paper>
        <DataGrid
          rows={stockItems}
          columns={columns}
          loading={loading}
          pageSizeOptions={[10, 25, 50]}
          initialState={{
            pagination: { paginationModel: { pageSize: 25 } },
          }}
          autoHeight
        />
      </Paper>

      {/* Add/Edit Dialog */}
      <Dialog open={openDialog} onClose={() => setOpenDialog(false)} maxWidth="md" fullWidth>
        <DialogTitle>{editingItem ? 'Edit Stock Item' : 'Add Stock Item'}</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12} sm={6}>
              <TextField
                label="SKU"
                fullWidth
                value={formData.sku || ''}
                onChange={(e) => setFormData({ ...formData, sku: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Name"
                fullWidth
                value={formData.name || ''}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                label="Description"
                fullWidth
                multiline
                rows={2}
                value={formData.description || ''}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth>
                <InputLabel>Category</InputLabel>
                <Select
                  value={formData.category || StockCategory.OTHER}
                  onChange={(e) => setFormData({ ...formData, category: e.target.value as StockCategory })}
                >
                  {Object.values(StockCategory).map((cat) => (
                    <MenuItem key={cat} value={cat}>
                      {cat.replace(/_/g, ' ')}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Unit"
                fullWidth
                value={formData.unit || ''}
                onChange={(e) => setFormData({ ...formData, unit: e.target.value })}
                placeholder="piece, meter, kg"
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField
                label="Unit Price"
                fullWidth
                type="number"
                value={formData.unitPrice || ''}
                onChange={(e) => setFormData({ ...formData, unitPrice: parseFloat(e.target.value) })}
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField
                label="Minimum Quantity"
                fullWidth
                type="number"
                value={formData.minimumQuantity || ''}
                onChange={(e) => setFormData({ ...formData, minimumQuantity: parseInt(e.target.value) })}
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField
                label="Reorder Level"
                fullWidth
                type="number"
                value={formData.reorderLevel || ''}
                onChange={(e) => setFormData({ ...formData, reorderLevel: parseInt(e.target.value) })}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                label="Barcode"
                fullWidth
                value={formData.barcode || ''}
                onChange={(e) => setFormData({ ...formData, barcode: e.target.value })}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
          <Button onClick={handleSave} variant="contained">
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default StockManagement;
