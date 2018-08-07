package mirage.net.services;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import mirage.utils.Config;

public class AWService extends AbstractVerticle {

	private Router router;
		
	@Override
	public void start(Future<Void> fut) {
		router = Router.router(vertx);
		router.route().handler(BodyHandler.create());

		initResources();

		vertx.executeBlocking(future -> {
			future.complete(vertx.createHttpServer());
		}, res -> {
			final HttpServer server = (HttpServer) res.result();
			
			vertx.executeBlocking(fut2 -> {
				fut2.complete(server.websocketHandler(AWServiceHandlers::handleTrackingRequest)
						.requestHandler(router::accept));
			}, res2 -> {
				server.listen(Config.WOAT_NODE_SERVICE_PORT, handler -> {	
					log(handler.succeeded() 
							? "Listening on port " + handler.result().actualPort() 
							: "Failed: " + handler.cause());
					
					if (!handler.succeeded()) {
						System.exit(0);
					}
				});
			});
		});
	}

	private void initResources() {		
//		router.get("/").handler(StaticHandler.create()).failureHandler(event -> {
//			event.response().sendFile();
//		});
		
		// AW > SERVICE
		router.get("/service").handler(AWServiceHandlers::handleGetServiceInfo);
		
		// GET INFRASTRUCTURE'S INFO
		router.get("/info").handler(AWServiceHandlers::handleGetInfrastructureInfo);

		// AW > ACTIONS
		//router.put("/aw/:name").handler(AWServiceHandlers::handleCreateAW);
		router.get("/aw/:awID/info").handler(AWServiceHandlers::handleGetAWInfo);
		
		// JOIN DI AGENTS
		router.post("/aw/:awID/agents").handler(AWServiceHandlers::handlePostAgentJoinAW);
		
		// QUIT AW
		router.delete("/aw/:awID/agents/:sessionID").handler(AWServiceHandlers::handleDeleteQuitAW);
		
		//JOIN DI USER
		router.post("/aw/:awID/users/sessions").handler(AWServiceHandlers::handlePostUserJoinAW);

		//DELETE DI USER
		router.delete("/aw/:awID/users/sessions/:sessionID").handler(AWServiceHandlers::handleDeleteQuitUser);
		
		//USERS API
		
		router.post("/aw/:awID/users/actions/updateGaze").handler(AWServiceHandlers::handleUserUpdateGaze);
		
		router.post("/aw/:awID/users/actions/updateLocation").handler(AWServiceHandlers::handleUserUpdateLocation);
		
		
		// AW > INFRASTUCTURAL AGENTS
		//router.post("/aw/:awID/agents/:agentName/registerpolicy").handler(AWServiceHandlers::handleAgentRegisterPolicy);
		//router.post("/aw/:awID/agents/:agentName/removepolicy/:policyID").handler(AWServiceHandlers::handleAgentRemovePolicy);

		// AW > ENTITIES
		router.get("/aw/:awID/entities").handler(AWServiceHandlers::handleGetEntities);
		
		// CREATE AE
		router.post("/aw/:awID/entities").handler(AWServiceHandlers::handleAddAugmentedEntity);
		
		// GET AE  
		router.get("/aw/:awID/entities/:entityID").handler(AWServiceHandlers::handleGetEntityByID);
		
		// DELETE AE
		router.delete("/aw/:awID/entities/:entityID").handler(AWServiceHandlers::handleRemoveEntityByID);
		
		//MOVE AE -- DISCUTERNE UTILITA' (Nobile)
		//router.post("/aw/awID/entities/:entityID/actions/moveEntity");
		
		// DEFINE REGION
		router.post("/aw/:awID/regions").handler(AWServiceHandlers::handleDefineRegion);
		
		//GET REGIONS DETAILS
		router.get("/aw/:awID/regions").handler(AWServiceHandlers::handleGetRegionsDetails);
		
		// GET REGION DETAILS
		router.get("/aw/:awID/regions/:regionID").handler(AWServiceHandlers::handleGetRegion);
		
		
		//GET AE MODEL/PROPERTIES/ACTIONS
		router.get("/aw/:awID/entities/:entityID/model").handler(AWServiceHandlers::handleGetEntityModel);
		router.get("/aw/:awID/entities/:entityID/model/:modelElement").handler(AWServiceHandlers::handleGetEntityModelElement);
		router.get("/aw/:awID/entities/:entityID/properties").handler(AWServiceHandlers::handleGetEntityProperties);
		router.get("/aw/:awID/entities/:entityID/properties/:property").handler(AWServiceHandlers::handleGetEntityProperty);
		router.put("/aw/:awID/entities/:entityID/properties/:property").handler(AWServiceHandlers::handleSetEntityProperty);
		//router.post("/aw/:awID/entities/:entityID/properties/holograms/:hologramID/actions/:action").handler(AWServiceHandlers::handleDoActionOnEntityHologram);
		router.get("/aw/:awID/entities/:entityID/actions").handler(AWServiceHandlers::handleGetEntityActions);
		router.post("/aw/:awID/entities/:entityID/actions/:action").handler(AWServiceHandlers::handleDoActionOnEntity);
	
		// AW > MESSAGES
		//router.get("/aw/:awID/messages/:queueID").handler(AWServiceHandlers::handleMessageQueueStatus);
		//router.get("/aw/:awID/messages/:queueID/pop").handler(AWServiceHandlers::handlePopMessageFromQueue);
		//router.put("/aw/:awID/messages/:queueID/push").handler(AWServiceHandlers::handlePushMessageToQueue);
	}
	
	private void log(String msg) {
		LoggerFactory.getLogger(AWService.class).info("[" + AWService.class.getSimpleName() + "] " + msg);
	}
}
