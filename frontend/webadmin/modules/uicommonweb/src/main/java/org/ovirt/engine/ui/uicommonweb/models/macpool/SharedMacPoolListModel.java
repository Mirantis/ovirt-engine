package org.ovirt.engine.ui.uicommonweb.models.macpool;

import java.util.ArrayList;
import java.util.List;

import org.ovirt.engine.core.common.action.RemoveMacPoolByIdParameters;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.MacPool;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.help.HelpTag;
import org.ovirt.engine.ui.uicommonweb.models.ConfirmationModel;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListWithDetailsModel;
import org.ovirt.engine.ui.uicommonweb.models.configure.PermissionListModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.ObservableCollection;

public class SharedMacPoolListModel extends ListWithDetailsModel {

    private static final String CMD_REMOVE = "OnRemove"; //$NON-NLS-1$
    private static final String CMD_CANCEL = "Cancel"; //$NON-NLS-1$

    private UICommand newCommand;
    private UICommand editCommand;
    private UICommand removeCommand;

    public UICommand getNewCommand() {
        return newCommand;
    }

    @Override
    public UICommand getEditCommand() {
        return editCommand;
    }

    public UICommand getRemoveCommand() {
        return removeCommand;
    }

    public SharedMacPoolListModel() {
        newCommand = new UICommand("New", this); //$NON-NLS-1$
        editCommand = new UICommand("Edit", this); //$NON-NLS-1$
        removeCommand = new UICommand("Remove", this); //$NON-NLS-1$
        setComparator(new Linq.SharedMacPoolComparator());

        updateActionAvailability();
    }

    @Override
    protected String getListName() {
        return "SharedMacPoolListModel"; //$NON-NLS-1$
    }

    @Override
    protected void syncSearch() {
        super.syncSearch(VdcQueryType.GetAllMacPools, new VdcQueryParametersBase());
    }

    private void updateActionAvailability() {
        getEditCommand().setIsExecutionAllowed(getSelectedItems() != null && getSelectedItems().size() == 1);

        boolean removeAllowed = true;
        if (getSelectedItems() == null || getSelectedItems().isEmpty()) {
            removeAllowed = false;
        } else {
            for (MacPool macPool : (Iterable<MacPool>) getSelectedItems()) {
                if (macPool.isDefaultPool()) {
                    removeAllowed = false;
                }
            }
        }
        getRemoveCommand().setIsExecutionAllowed(removeAllowed);
    }

    @Override
    protected void selectedItemsChanged() {
        super.selectedItemsChanged();
        updateActionAvailability();
    }

    private void newMacPool() {
        SharedMacPoolModel model = new NewSharedMacPoolModel(this);
        model.setEntity(new MacPool());
        setWindow(model);
    }

    private void editMacPool() {
        SharedMacPoolModel model = new SharedMacPoolModel(this, VdcActionType.UpdateMacPool);
        model.setTitle(ConstantsManager.getInstance().getConstants().editSharedMacPoolTitle());
        model.setHashName("edit_shared_mac_pool"); //$NON-NLS-1$
        model.setHelpTag(HelpTag.edit_shared_mac_pool);
        model.setEntity((MacPool) getSelectedItem());
        setWindow(model);
    }

    private void removeMacPools() {
        ConfirmationModel model = new ConfirmationModel();
        model.setTitle(ConstantsManager.getInstance().getConstants().removeSharedMacPoolsTitle());
        model.setHashName("remove_shared_mac_pools"); //$NON-NLS-1$
        model.setHelpTag(HelpTag.remove_shared_mac_pools);

        UICommand tempVar = new UICommand(CMD_REMOVE, this); //$NON-NLS-1$
        tempVar.setTitle(ConstantsManager.getInstance().getConstants().ok());
        tempVar.setIsDefault(true);
        model.getCommands().add(tempVar);
        UICommand tempVar2 = new UICommand(CMD_CANCEL, this); //$NON-NLS-1$
        tempVar2.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        tempVar2.setIsCancel(true);
        model.getCommands().add(tempVar2);

        List<String> macPoolNames = new ArrayList<String>();
        for (MacPool macPool : (Iterable<MacPool>) getSelectedItems()) {
            macPoolNames.add(macPool.getName());
        }
        model.setItems(macPoolNames);

        setConfirmWindow(model);
    }

    private void cancel() {
        setConfirmWindow(null);
    }

    private void onRemove() {
        cancel();
        ArrayList<VdcActionParametersBase> params = new ArrayList<VdcActionParametersBase>();
        for (MacPool macPool : (Iterable<MacPool>) getSelectedItems()) {
            params.add(new RemoveMacPoolByIdParameters(macPool.getId()));
        }
        Frontend.getInstance().runMultipleAction(VdcActionType.RemoveMacPool, params);
    }

    @Override
    public void executeCommand(UICommand command) {
        super.executeCommand(command);

        if (command == getNewCommand()) {
            newMacPool();
        } else if (command == getEditCommand()) {
            editMacPool();
        } else if (command == getRemoveCommand()) {
            removeMacPools();
        } else if (CMD_REMOVE.equals(command.getName())) {
            onRemove();
        } else if (CMD_CANCEL.equals(command.getName())) {
            cancel();
        }
    }

    @Override
    protected void initDetailModels() {
        ObservableCollection<EntityModel> list = new ObservableCollection<EntityModel>();
        list.add(new PermissionListModel());

        setDetailModels(list);
    }
}
