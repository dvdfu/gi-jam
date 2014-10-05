package com.dvdfu.gijam.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.dvdfu.gijam.MainGame;
import com.dvdfu.gijam.handlers.Consts;
import com.dvdfu.gijam.handlers.Enums;
import com.dvdfu.gijam.handlers.GameStage;
import com.dvdfu.gijam.handlers.Input;
import com.dvdfu.gijam.handlers.ObjectPool;
import com.dvdfu.gijam.objects.Background;
import com.dvdfu.gijam.objects.Block;
import com.dvdfu.gijam.objects.Chaser;
import com.dvdfu.gijam.objects.Particle;
import com.dvdfu.gijam.objects.PowerUp;

public class GameScreen extends AbstractScreen {
	private GameStage stage;
	private Background bg;
	private ObjectPool pool;
	private Chaser chaser;
	private Group blocks;
	private Group powerUps;
	private Group particles;

	private Block currentBlock;
	private float origX;
	private float origY;

	private float newX;
	private float newY;
	private float savedX;
	private float savedY;

	private float MaxLeftG = 500f;
	private float MaxRightG = 40000f;
	private float LeftGauge = 500f;
	private float RightGauge = 40000f;

	private float width;
	private float height;

	private float MaxAWidth;
	private float MaxAHeight;

	private float minDimSize = 50f;

	private int powerUpCounter = MathUtils.random(300, 600);

	public GameScreen(MainGame game) {
		super(game);
		stage = new GameStage();
		pool = new ObjectPool(stage);
		bg = new Background(stage);
		stage.addActor(bg);

		chaser = new Chaser(stage);
		stage.addActor(chaser);
		blocks = new Group();
		stage.addActor(blocks);

		powerUps = new Group();
		stage.addActor(powerUps);

		particles = new Group();
		stage.addActor(particles);

		stage.setCamPosition(Gdx.graphics.getWidth() / 2,
				Gdx.graphics.getHeight() / 2);
	}

	public void render(float delta) {

		if (RightGauge < 0) {
			RightGauge = 0;
		} else if (RightGauge > MaxRightG) {
			RightGauge = MaxRightG;
		}
		GaugeController();
		System.out.println(LeftGauge + " : " + RightGauge);
		bg.update();
		chaser.update();
		powerUpController();
		blockController();
		stage.act(delta);
		stage.draw();
	}

	private void GaugeController() {
		if (LeftGauge < MaxLeftG) {
			LeftGauge += 0.05f;
		}
		if (RightGauge < MaxRightG) {
			RightGauge += 50f;
		}
	}

	private void powerUpController() {
		if (powerUpCounter > 0) {
			powerUpCounter--;
		} else {
			PowerUp powerUp = pool.getPowerUp();
			powerUps.addActor(powerUp);
			powerUpCounter = MathUtils.random(300, 600);
		}

		for (int i = 0; i < powerUps.getChildren().size; i++) {
			PowerUp powerUp = (PowerUp) powerUps.getChildren().get(i);
			powerUp.update();
			chaser.collidePowerUp(powerUp);
			if (powerUp.isDead()) {
				powerUps.removeActor(powerUp);
				pool.free(powerUp);
			}
		}
	}

	private void blockController() {
		for (int i = 0; i < blocks.getChildren().size; i++) {
			Block block = (Block) blocks.getChildren().get(i);
			block.update();
			chaser.collideBlock(block);
			for (int j = i + 1; j < blocks.getChildren().size; j++) {
				Block other = (Block) blocks.getChildren().get(j);
				block.collideBlock(other);
				other.collideBlock(block);
			}
			if (block.isDead()) {
				for (float j = block.getX(); j < block.getRight(); j += 32) {
					for (float k = block.getY(); k < block.getTop(); k += 32) {
						Particle particle = pool.getParticle();
						particle.setType(Enums.ParticleType.ROCK);
						particle.setPosition(j, k);
						particles.addActor(particle);
					}
				}
				blocks.removeActor(block);
				pool.free(block);
			}
		}

		for (Actor actor : particles.getChildren()) {
			Particle particle = (Particle) actor;
			if (particle.dead()) {
				particles.removeActor(actor);
				pool.free(particle);
			}
		}

		if (Input.MousePressed() && currentBlock == null) {
			origX = MathUtils.clamp(Input.MouseX(), Consts.DrawAreaRight,
					Consts.ScreenWidth);
			origY = MathUtils.clamp(Input.MouseY(), 0, Consts.ScreenHeight);

			currentBlock = pool.getBlock();
			blocks.addActor(currentBlock);
		} else if (Input.MouseDown() && currentBlock != null) {
			origX -= Consts.ScreenSpeed;

			newX = MathUtils.clamp(Input.MouseX(), Consts.DrawAreaRight,
					Consts.ScreenWidth);
			newY = MathUtils.clamp(Input.MouseY(), 0, Consts.ScreenHeight);

			width = Math.abs(newX - origX);
			height = Math.abs(newY - origY);

			width = MathUtils.clamp(width, minDimSize, Consts.ScreenWidth);
			height = MathUtils.clamp(height, minDimSize, Consts.ScreenHeight);

			if (newX > origX) {
				newX = origX + width;
			} else if (newX < origX) {
				newX = origX - width;
			}
			if (newY > origY) {
				newY = origY + height;
			} else if (newY < origY) {
				newY = origY - height;
			}

			if (width * height <= RightGauge) {
				savedX = newX;
				savedY = newY;
				MaxAHeight = height;
				MaxAWidth = width;
				currentBlock.setPosition(Math.min(newX, origX),
						Math.min(newY, origY));
				currentBlock.setSize(width, height);
			}

		} else if (Input.MouseReleased() && currentBlock != null) {
			if (width * height <= RightGauge) {
				currentBlock.createBlock();
				RightGauge -= (width * height);
			} else if (RightGauge >= 2500) {
				currentBlock.setSize(MaxAWidth, MaxAHeight);
				currentBlock.setPosition(Math.min(savedX, origX),
						Math.min(savedY, origY));
				currentBlock.createBlock();
				RightGauge -= (MaxAWidth * MaxAHeight);
				MaxAHeight = minDimSize;
				MaxAWidth = minDimSize;
				// blocks.removeActor(currentBlock);
				// pool.free(currentBlock);
			}
			currentBlock = null;
		}
	}

	public void resize(int width, int height) {
	}

	public void show() {
	}

	public void hide() {
	}

	public void pause() {
	}

	public void resume() {
	}

	public void dispose() {
		stage.dispose();
	}

	public ObjectPool getPool() {
		return pool;
	}
}
