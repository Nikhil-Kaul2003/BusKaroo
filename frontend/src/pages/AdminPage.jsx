import { useCallback, useEffect, useState } from 'react';
import { Button, Card } from '../components/common';
import { adminApi } from '../api/admin';
import { getApiErrorMessage } from '../api/client';
import './AdminPage.css';
const emptyCategory = { id: null, name: '', description: '' };
const emptyBus = {
    id: null,
    registrationNumber: '',
    model: '',
    manufacturer: '',
    year: new Date().getFullYear(),
    totalSeats: 40,
    status: 'ACTIVE',
    categoryId: '',
};
const makeSeatLayout = (totalSeats, categoryName) => {
    const category = categoryName.toLowerCase();
    const seatsPerRow = category.includes('sleeper') ? 2 : category.includes('luxury') ? 3 : 4;
    const layout = category.includes('sleeper') ? '1+1 sleeper berths' : category.includes('luxury') ? '2+1 premium' : '2+2 non-AC';
    return JSON.stringify({
        rows: Math.ceil(totalSeats / seatsPerRow),
        seatsPerRow,
        layout,
        totalSeats,
        airConditioned: category.includes('luxury') || category.includes('sleeper'),
        categories: { [category.includes('luxury') ? 'PREMIUM' : category.includes('sleeper') ? 'BERTH' : 'STANDARD']: Array.from({ length: category.includes('luxury') ? Math.min(totalSeats, seatsPerRow) : totalSeats }, (_, index) => String(index + 1)) },
    });
};
export const AdminPage = () => {
    const [tab, setTab] = useState('buses');
    const [categories, setCategories] = useState([]);
    const [buses, setBuses] = useState([]);
    const [categoryForm, setCategoryForm] = useState(emptyCategory);
    const [busForm, setBusForm] = useState(emptyBus);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');
    const [notice, setNotice] = useState('');
    const loadData = useCallback(async () => {
        setLoading(true);
        setError('');
        try {
            const [categoryData, busData] = await Promise.all([
                adminApi.getCategories(),
                adminApi.getBuses(),
            ]);
            setCategories(categoryData);
            setBuses(busData);
        }
        catch (loadError) {
            setError(getApiErrorMessage(loadError, 'Could not load the admin data.'));
        }
        finally {
            setLoading(false);
        }
    }, []);
    useEffect(() => { void loadData(); }, [loadData]);
    const saveCategory = async (event) => {
        event.preventDefault();
        setSaving(true);
        setError('');
        setNotice('');
        try {
            const payload = { name: categoryForm.name.trim(), description: categoryForm.description.trim() || null };
            if (categoryForm.id)
                await adminApi.updateCategory(categoryForm.id, payload);
            else
                await adminApi.createCategory(payload);
            setCategoryForm(emptyCategory);
            setNotice('Bus category saved.');
            await loadData();
        }
        catch (saveError) {
            setError(getApiErrorMessage(saveError, 'Could not save this category.'));
        }
        finally {
            setSaving(false);
        }
    };
    const saveBus = async (event) => {
        event.preventDefault();
        if (busForm.categoryId === '')
            return;
        setSaving(true);
        setError('');
        setNotice('');
        try {
            const payload = {
                registrationNumber: busForm.registrationNumber.trim().toUpperCase(),
                model: busForm.model.trim(),
                manufacturer: busForm.manufacturer.trim(),
                year: Number(busForm.year),
                totalSeats: Number(busForm.totalSeats),
                status: busForm.status,
                categoryId: Number(busForm.categoryId),
                seatLayout: makeSeatLayout(Number(busForm.totalSeats), categories.find(category => category.id === Number(busForm.categoryId))?.name || 'Normal'),
            };
            if (busForm.id)
                await adminApi.updateBus(busForm.id, payload);
            else
                await adminApi.createBus(payload);
            setBusForm(emptyBus);
            setNotice('Bus record saved.');
            await loadData();
        }
        catch (saveError) {
            setError(getApiErrorMessage(saveError, 'Could not save this bus.'));
        }
        finally {
            setSaving(false);
        }
    };
    const deleteCategory = async (category) => {
        if (!window.confirm(`Delete the “${category.name}” category? Categories used by buses cannot be deleted.`))
            return;
        setError('');
        setNotice('');
        try {
            await adminApi.deleteCategory(category.id);
            if (categoryForm.id === category.id)
                setCategoryForm(emptyCategory);
            setNotice('Bus category deleted.');
            await loadData();
        }
        catch (deleteError) {
            setError(getApiErrorMessage(deleteError, 'Could not delete this category. Reassign its buses first.'));
        }
    };
    const deleteBus = async (bus) => {
        if (!window.confirm(`Delete bus ${bus.registrationNumber}? Buses assigned to trips cannot be deleted.`))
            return;
        setError('');
        setNotice('');
        try {
            await adminApi.deleteBus(bus.id);
            if (busForm.id === bus.id)
                setBusForm(emptyBus);
            setNotice('Bus deleted.');
            await loadData();
        }
        catch (deleteError) {
            setError(getApiErrorMessage(deleteError, 'Could not delete this bus.'));
        }
    };
    const startEditBus = (bus) => {
        setTab('buses');
        setBusForm({
            id: bus.id,
            registrationNumber: bus.registrationNumber,
            model: bus.model,
            manufacturer: bus.manufacturer,
            year: bus.year,
            totalSeats: bus.totalSeats,
            status: bus.status,
            categoryId: bus.categoryId ?? '',
        });
        window.scrollTo({ top: 0, behavior: 'smooth' });
    };
    return (<div className="admin-page page">
      <header className="admin-page-header">
        <div>
          <span className="admin-eyebrow">BUSKAROO OPERATIONS</span>
          <h1>Admin dashboard</h1>
          <p>Manage fleet records and the categories used to organise buses.</p>
        </div>
        <Button variant="secondary" onClick={() => void loadData()} loading={loading}>Refresh data</Button>
      </header>

      <div className="admin-summary-grid">
        <Card className="admin-stat-card"><span>Fleet buses</span><strong>{buses.length}</strong></Card>
        <Card className="admin-stat-card"><span>Bus categories</span><strong>{categories.length}</strong></Card>
        <Card className="admin-stat-card"><span>Active buses</span><strong>{buses.filter(bus => bus.status === 'ACTIVE').length}</strong></Card>
      </div>

      <div className="admin-tabs" role="tablist" aria-label="Admin resources">
        <button className={tab === 'buses' ? 'active' : ''} onClick={() => setTab('buses')} role="tab" aria-selected={tab === 'buses'}>Buses</button>
        <button className={tab === 'categories' ? 'active' : ''} onClick={() => setTab('categories')} role="tab" aria-selected={tab === 'categories'}>Bus categories</button>
      </div>

      {error && <div className="admin-message error" role="alert">{error}</div>}
      {notice && <div className="admin-message success" role="status">{notice}</div>}

      {tab === 'categories' ? (<div className="admin-content-grid">
          <Card className="admin-form-card">
            <h2>{categoryForm.id ? 'Edit category' : 'Create category'}</h2>
            <form onSubmit={saveCategory} className="admin-form">
              <label>Category name<input required maxLength={80} value={categoryForm.name} onChange={event => setCategoryForm({ ...categoryForm, name: event.target.value })} placeholder="e.g. AC Sleeper"/></label>
              <label>Description<textarea maxLength={250} value={categoryForm.description} onChange={event => setCategoryForm({ ...categoryForm, description: event.target.value })} placeholder="Describe the bus category" rows={3}/></label>
              <div className="admin-form-actions">
                <Button type="submit" loading={saving}>{categoryForm.id ? 'Save changes' : 'Add category'}</Button>
                {categoryForm.id && <Button type="button" variant="secondary" onClick={() => setCategoryForm(emptyCategory)}>Cancel</Button>}
              </div>
            </form>
          </Card>
          <Card className="admin-table-card">
            <div className="admin-table-heading"><h2>Categories</h2><span>{categories.length} total</span></div>
            {loading ? <p className="admin-empty">Loading categories…</p> : categories.length === 0 ? <p className="admin-empty">No categories yet. Create one to start categorising the fleet.</p> : (<div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Name</th><th>Description</th><th>Actions</th></tr></thead><tbody>
                {categories.map(category => <tr key={category.id}><td className="admin-strong">{category.name}</td><td>{category.description || '—'}</td><td><div className="admin-row-actions"><button onClick={() => setCategoryForm({ id: category.id, name: category.name, description: category.description || '' })}>Edit</button><button className="danger" onClick={() => void deleteCategory(category)}>Delete</button></div></td></tr>)}
              </tbody></table></div>)}
          </Card>
        </div>) : (<div className="admin-content-grid">
          <Card className="admin-form-card">
            <h2>{busForm.id ? 'Edit bus' : 'Add a bus'}</h2>
            {categories.length === 0 && <p className="admin-inline-hint">Create a bus category before adding a bus.</p>}
            <form onSubmit={saveBus} className="admin-form">
              <label>Registration number<input required maxLength={20} value={busForm.registrationNumber} onChange={event => setBusForm({ ...busForm, registrationNumber: event.target.value })} placeholder="MH-01-AB-1234"/></label>
              <div className="admin-form-row"><label>Manufacturer<input required maxLength={100} value={busForm.manufacturer} onChange={event => setBusForm({ ...busForm, manufacturer: event.target.value })} placeholder="Volvo"/></label><label>Model<input required maxLength={100} value={busForm.model} onChange={event => setBusForm({ ...busForm, model: event.target.value })} placeholder="9600 B8R"/></label></div>
              <div className="admin-form-row"><label>Category<select required value={busForm.categoryId} onChange={event => setBusForm({ ...busForm, categoryId: event.target.value ? Number(event.target.value) : '' })}><option value="">Choose category</option>{categories.map(category => <option key={category.id} value={category.id}>{category.name}</option>)}</select></label><label>Status<select value={busForm.status} onChange={event => setBusForm({ ...busForm, status: event.target.value })}><option value="ACTIVE">Active</option><option value="MAINTENANCE">Maintenance</option><option value="RETIRED">Retired</option></select></label></div>
              <div className="admin-form-row"><label>Year<input type="number" min={2000} max={2035} required value={busForm.year} onChange={event => setBusForm({ ...busForm, year: Number(event.target.value) })}/></label><label>Seats<input type="number" min={1} max={100} required value={busForm.totalSeats} onChange={event => setBusForm({ ...busForm, totalSeats: Number(event.target.value) })}/></label></div>
              <p className="admin-inline-hint">A 2+2 seat layout is generated from the capacity entered above.</p>
              <div className="admin-form-actions"><Button type="submit" loading={saving} disabled={categories.length === 0}>{busForm.id ? 'Save changes' : 'Add bus'}</Button>{busForm.id && <Button type="button" variant="secondary" onClick={() => setBusForm(emptyBus)}>Cancel</Button>}</div>
            </form>
          </Card>
          <Card className="admin-table-card">
            <div className="admin-table-heading"><h2>Fleet</h2><span>{buses.length} buses</span></div>
            {loading ? <p className="admin-empty">Loading fleet…</p> : buses.length === 0 ? <p className="admin-empty">No bus records found.</p> : (<div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Bus</th><th>Category</th><th>Seats</th><th>Status</th><th>Actions</th></tr></thead><tbody>
                {buses.map(bus => <tr key={bus.id}><td><span className="admin-strong">{bus.registrationNumber}</span><small>{bus.manufacturer} {bus.model} · {bus.year}</small></td><td>{bus.categoryName || 'Uncategorized'}</td><td>{bus.totalSeats}</td><td><span className={`admin-status ${bus.status.toLowerCase()}`}>{bus.status}</span></td><td><div className="admin-row-actions"><button onClick={() => startEditBus(bus)}>Edit</button><button className="danger" onClick={() => void deleteBus(bus)}>Delete</button></div></td></tr>)}
              </tbody></table></div>)}
          </Card>
        </div>)}
    </div>);
};
export default AdminPage;
