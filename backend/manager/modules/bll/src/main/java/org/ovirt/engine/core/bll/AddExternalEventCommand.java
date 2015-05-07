package org.ovirt.engine.core.bll;

import java.util.List;

import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.AddExternalEventParameters;
import org.ovirt.engine.core.common.businessentities.ActionGroup;
import org.ovirt.engine.core.common.businessentities.AuditLog;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AlertDirector;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBase;
import org.ovirt.engine.core.dao.VdsDynamicDAO;

import javax.inject.Inject;

public class AddExternalEventCommand<T extends AddExternalEventParameters> extends ExternalEventCommandBase<T> {
    private static final String OVIRT="oVirt";

    @Inject VdsDynamicDAO hostDao;

    public AddExternalEventCommand(T parameters) {
        super(parameters);
    }

    @Override
    protected boolean canDoAction() {
        boolean result=true;
        if (getParameters().getEvent() == null || getParameters().getEvent().getOrigin().equalsIgnoreCase(OVIRT)){
            addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_EXTERNAL_EVENT_ILLEGAL_ORIGIN);
            result = false;
        }
        if (!result) {
            addCanDoActionMessage(VdcBllMessages.VAR__ACTION__ADD);
            addCanDoActionMessage(VdcBllMessages.VAR__TYPE__EXTERNAL_EVENT);
        }
        return result;
    }
    @Override
    protected void executeCommand() {
        AuditLogableBase event = new AuditLogableBase(getParameters().getEvent());
        event.setExternal(true);
        String message = getParameters().getEvent().getMessage();
        switch (getParameters().getEvent().getSeverity()){
            case NORMAL:
                auditLogDirector.log(event, AuditLogType.EXTERNAL_EVENT_NORMAL, message);
                break;
            case WARNING:
                auditLogDirector.log(event, AuditLogType.EXTERNAL_EVENT_WARNING, message);
                break;
            case ERROR:
                auditLogDirector.log(event, AuditLogType.EXTERNAL_EVENT_ERROR, message);
                break;
            case ALERT:
                AlertDirector.Alert(event, AuditLogType.EXTERNAL_ALERT, auditLogDirector, message);
                break;
        }
        AuditLog auditLog = DbFacade.getInstance().getAuditLogDao().getByOriginAndCustomEventId(getParameters().getEvent().getOrigin(), getParameters().getEvent().getCustomEventId());
        if (auditLog != null) {
            setActionReturnValue(auditLog.getAuditLogId());
            setSucceeded(true);
        }
        // Update host external status if set
        if (hasHostExternalStatus()) {
            hostDao.updateExternalStatus(getParameters().getEvent().getVdsId(), getParameters().getExternalStatus());
        }
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        List<PermissionSubject> permissionList = getPermissionList(getParameters().getEvent());
        // check for external host status modification
        if (hasHostExternalStatus()) {
            permissionList.add(new PermissionSubject(getParameters().getEvent().getVdsId(),
                    VdcObjectType.VDS, ActionGroup.EDIT_HOST_CONFIGURATION));
        }
        return permissionList;
    }

    private boolean hasHostExternalStatus() {
        return getParameters().getEvent().getVdsId() != null && getParameters().getExternalStatus() != null;
    }
}
