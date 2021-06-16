
package edu.ucla.library.iiif.auth.verticles;

import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

import info.freelibrary.util.Constants;
import info.freelibrary.util.FileUtils;
import info.freelibrary.util.Logger;
import info.freelibrary.util.PortUtils;

import edu.ucla.library.iiif.auth.Config;
import edu.ucla.library.iiif.auth.MessageCodes;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;

/**
 * An abstract test that other tests can extend.
 */
public abstract class AbstractBouncerTest {

    /**
     * Rule that creates the test context.
     */
    @Rule
    public RunTestOnContext myContext = new RunTestOnContext();

    /**
     * Rule that provides access to the test method name.
     */
    @Rule
    public TestName myNames = new TestName();

    /**
     * The port at which our test instances listen.
     */
    protected int myPort;

    /**
     * Sets up the test.
     *
     * @param aContext A test context
     */
    @Before
    public void setUp(final TestContext aContext) {
        final DeploymentOptions options = new DeploymentOptions();
        final Async asyncTask = aContext.async();
        final Vertx vertx = myContext.vertx();

        ConfigRetriever.create(vertx).getConfig().onSuccess(config -> {
            myPort = PortUtils.getPort();
            options.setConfig(config.put(Config.HTTP_PORT, myPort));

            vertx.deployVerticle(MainVerticle.class.getName(), options).onSuccess(result -> complete(asyncTask))
                    .onFailure(error -> aContext.fail(error));
        }).onFailure(error -> aContext.fail(error));
    }

    /**
     * Returns the logger used by an extending test.
     *
     * @return The test's logger
     */
    protected abstract Logger getLogger();

    /**
     * Undeploy a verticle so that a test can swap in a mock.
     *
     * @param aVerticleName The name of the verticle to undeploy
     * @return A future removal of the verticle
     */
    protected Future<Void> undeployVerticle(final String aVerticleName) {
        final Promise<Void> promise = Promise.promise();
        final Vertx vertx = myContext.vertx();

        vertx.sharedData().getLocalAsyncMap(MainVerticle.VERTICLES_MAP).onSuccess(map -> {
            map.get(aVerticleName).onSuccess(deploymentID -> {
                vertx.undeploy(deploymentID.toString()).onSuccess(result -> {
                    getLogger().debug(MessageCodes.BNCR_002, aVerticleName, deploymentID);
                    promise.complete();
                }).onFailure(error -> promise.fail(error));
            }).onFailure(error -> promise.fail(error));
        }).onFailure(error -> promise.fail(error));

        return promise.future();
    }

    /**
     * A convenience method to end asynchronous tasks.
     *
     * @param aAsyncTask A task to complete
     */
    protected void complete(final Async aAsyncTask) {
        if (!aAsyncTask.isCompleted()) {
            aAsyncTask.complete();
        }
    }

    /**
     * Takes a supplied file path and makes it unique so it can be used across different simultaneous tests.
     *
     * @param aFilePath A full file path
     * @return A unique version of the supplied file path
     */
    protected String getUniqueFileName(final String aFilePath) {
        final String filePath = FileUtils.stripExt(aFilePath);
        final String fileExt = FileUtils.getExt(aFilePath);

        return filePath + UUID.randomUUID().toString() + Constants.PERIOD + fileExt;
    }
}
