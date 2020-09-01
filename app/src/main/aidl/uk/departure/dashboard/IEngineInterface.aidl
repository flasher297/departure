package uk.departure.dashboard;

import uk.departure.dashboard.IEngineCallback;

interface IEngineInterface {

    void registerCallback(IEngineCallback cb);

    void unregisterCallback(IEngineCallback cb);
}
