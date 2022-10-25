package com.multplayergame.game;

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.multplayergame.sprite.PlayerShip;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class Multplayer extends ApplicationAdapter {
	private final float UPDATE_TIME = 1/60f;
	
	float timer;
	
	private Stage stage;
	private Table table;
	
	SpriteBatch batch;
	private Socket socket;
	private PlayerShip player;
	private Texture playerShip;
	private Texture friendlyShip;
	private HashMap<String, PlayerShip> friendlyPlayers;
	
	@Override
	public void create () {
		stage = new Stage(new StretchViewport(600, 360));
		Gdx.input.setInputProcessor(stage);
		
		TextButtonStyle btnStyle = new TextButtonStyle();
		btnStyle.font = new BitmapFont();
		btnStyle.up = null;
		btnStyle.down = null;
		
		TextButton button = new TextButton("", btnStyle);
		button.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				player.setPosition(player.getX() + (-200 * Gdx.graphics.getDeltaTime()), player.getY());
			}
		});
		
		TextButton button2 = new TextButton("", btnStyle);
		button2.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				player.setPosition(player.getX() + (200 * Gdx.graphics.getDeltaTime()), player.getY());
			}
		});
		
		table = new Table();
		table.debug();
		table.setFillParent(true);
		table.add(button).left().expand().fill();
		table.add(button2).right().expand().fill();
		
		stage.addActor(table);
		
		batch = new SpriteBatch();
		playerShip = new Texture("player1Ship.png");
		friendlyShip = new Texture("player2Ship.png");
		
		friendlyPlayers = new HashMap<String, PlayerShip>();
		
		connectSocket();
		configSocketEvents();
	}

	public void handleInput(float dt) {
		if (player != null) {
			if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
				player.setPosition(player.getX() + (-200 * dt), player.getY());
			} else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
				player.setPosition(player.getX() + (	200 * dt), player.getY());
			}			
		}
	}
	
	public void updateServer(float dt) {
		timer += dt;
		if (timer >= UPDATE_TIME && player != null && player.hasMoved()) {
			JSONObject data = new JSONObject();
			try {
				data.put("x", player.getX());
				data.put("y", player.getY());
				socket.emit("playerMoved", data);
			} catch (JSONException e ) {
				Gdx.app.log("Error", "Error sending object data");
			}
		}
	}
	
	@Override
	public void render () {
		Gdx.gl.glClearColor(.3f, .3f, .6f, 1);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
		handleInput(Gdx.graphics.getDeltaTime());
		
		updateServer(Gdx.graphics.getDeltaTime());
		
		batch.begin();
		if (player != null) player.draw(batch);
		for (HashMap.Entry<String, PlayerShip> entry : friendlyPlayers.entrySet()) {
			entry.getValue().draw(batch);
		}
		batch.end();
		
		stage.draw();
		stage.act();
	}
	
	@Override
	public void dispose () {	
		batch.dispose();
		playerShip.dispose();
		friendlyShip.dispose();
	}
	
	public void connectSocket() {
		try {
			socket = IO.socket("https://server-multiplayer-test.herokuapp.com/");
			socket.connect();
		} catch (Exception e) {
			Gdx.app.log("Error", e+"");
		}
	}
	
	public void configSocketEvents() {
		socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
			@Override
			public void call(Object... args) {
				Gdx.app.log("SocketIo", "Connected");
				player = new PlayerShip(playerShip);
			}
		}).on("socketId", new Emitter.Listener() {
			@Override
			public void call(Object... args) {
				JSONObject data = (JSONObject) args[0];
				try {
					String id = data.getString("id");
					Gdx.app.log("SocketIo", "my id: " + id);
				} catch (JSONException e) {
					Gdx.app.log("Error", "Error getting id");
				}
			}
		}).on("newPlayer", new Emitter.Listener() {
			@Override
			public void call(Object... args) {
				JSONObject data = (JSONObject) args[0];
				try {
					String id = data.getString("id");
					Gdx.app.log("SocketIo", "new player connected: " + id);
					friendlyPlayers.put(id, new PlayerShip(friendlyShip));
				} catch (JSONException e) {
					Gdx.app.log("Error", "Error getting new player id");
				}
			}
		}).on("playerDisconnected", new Emitter.Listener() {
			@Override
			public void call(Object... args) {
				JSONObject data = (JSONObject) args[0];
				try {
					String id = data.getString("id");
					friendlyPlayers.remove(id);
				} catch (JSONException e) {
					Gdx.app.log("Error", "Error getting disconnected player id");
				}
			}
		}).on("playerMoved", new Emitter.Listener() {
			@Override
			public void call(Object... args) {
				JSONObject data = (JSONObject) args[0];
				try {
					String id = data.getString("id");
					Double x = data.getDouble("x");
					Double y = data.getDouble("y");
					if (friendlyPlayers.get(id) != null) {
						friendlyPlayers.get(id).setPosition(x.floatValue(), y.floatValue());
					}
				} catch (JSONException e) {
					Gdx.app.log("Error", "Error handle player moved");
				}
			}
		}).on("getPlayers", new Emitter.Listener() {
			@Override
			public void call(Object... args) {
				JSONArray objects = (JSONArray) args[0];
				try {
					for (int i = 0; i < objects.length(); i ++) {
						PlayerShip coop = new PlayerShip(friendlyShip);
						Vector2 position = new Vector2();
						position.x = ((Double) objects.getJSONObject(i).getDouble("x")).floatValue();
						position.y = ((Double) objects.getJSONObject(i).getDouble("y")).floatValue();
						coop.setPosition(position.x, position.y);
						
						friendlyPlayers.put(objects.getJSONObject(i).getString("id"), coop);
					}
				} catch (JSONException e) {
					Gdx.app.log("Error", "Erro getting new player id");
				}
			}
		});
	}
}
