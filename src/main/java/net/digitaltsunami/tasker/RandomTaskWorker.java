package net.digitaltsunami.tasker;

import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class RandomTaskWorker implements TaskWorker {
    Random random = new Random();
    @Override
    public boolean processTask(String work) {
        if (random.nextInt(10) % 3 == 0)  {
            return false; // simulate error
        }
        return true;
    }
}
