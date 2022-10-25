package com.multplayergame.sprite;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;

public class PlayerShip extends Sprite {
	private Vector2 previousPosition;
	
	public PlayerShip(Texture texture) {
		super(texture);
		previousPosition = new Vector2(getX(), getY());
	}
	
	public boolean hasMoved() {
		if (previousPosition.x != getX() || previousPosition.y != getY()) {
			previousPosition.x = getX();
			previousPosition.y = getY();
			return true;
		}
		return false;
	}
}
