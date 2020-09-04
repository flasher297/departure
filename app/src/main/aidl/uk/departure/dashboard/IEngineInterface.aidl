package uk.departure.dashboard;

import uk.departure.dashboard.IEngineCallback;

interface IEngineInterface {

    void registerOutcomeCallback(IEngineCallback cb);

    void unregisterOutcomeCallback(IEngineCallback cb);
}
