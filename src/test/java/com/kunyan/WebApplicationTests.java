package com.kunyan;

import com.kunyan.entity.IdentifyException;
import com.kunyan.service.ProjectService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RunWith(SpringRunner.class)
@SpringBootTest

public class WebApplicationTests {
	private Log logger = LogFactory.getLog(WebApplicationTests.class);
	@Autowired
	private ProjectService projectService;
	@Test
	public void testThread() {
		ExecutorService service = Executors.newFixedThreadPool(1);
			service.execute(new Runnable() {
				@Override
				public void run() {
					int a = 5 / 0;
					System.out.println(a);
					try {
						Thread.sleep(1000 * 10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			});


			service.execute(new Runnable() {
				@Override
				public void run() {
					int a = 5 / 0;
					System.out.println(a);
					try {
						Thread.sleep(1000 * 10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			});

			System.out.println("haha");
		try {
			Thread.sleep(1000* 20);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

    @Test
    public void testDetect() {
		try {
			BufferedImage image = ImageIO.read(new File("d:\\w1.jpg"));
			projectService.checkImage(image);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (IdentifyException e) {
			e.printStackTrace();
		}
	}
}
