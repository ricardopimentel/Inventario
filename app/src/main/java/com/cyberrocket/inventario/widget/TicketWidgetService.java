package com.cyberrocket.inventario.widget;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class TicketWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new TicketRemoteViewsFactory(this.getApplicationContext(), intent);
    }
}
