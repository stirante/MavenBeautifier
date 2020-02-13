package com.stirante.mavenBeautifier;

import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.project.DependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.apache.maven.shared.utils.logging.MessageUtils;
import org.eclipse.aether.RepositoryEvent;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.slf4j.impl.SimpleLogger;

import javax.inject.Named;
import java.io.*;
import java.lang.reflect.Field;

@Named
public class MavenBeautifier extends AbstractEventSpy {

    private static final String[] ANIMATION = {
            "[|] ",
            "[/] ",
            "[-] ",
            "[\\] "
    };
    private static final String LOG_FILE_KEY = "com.stirante.maven.logFileKey";
    private static final String NO_LOG_FILE = "none";
    public static final String FAILED = "[X] ";
    public static final String SUCCEEDED = "[✓] ";

    private final Thread thread;
    private volatile String status = "";
    private volatile boolean error = false;
    private volatile boolean finished = false;
    private int animationIndex = 0;

    public MavenBeautifier() {
        AnsiConsole.systemInstall();
        MessageUtils.setColorEnabled(false);
        try {
            String logFile = new File("").getAbsoluteFile().getName() + ".log";
            try {
                String logFileProp = System.getProperty(LOG_FILE_KEY);
                if (logFileProp != null) {
                    logFile = logFileProp;
                }
            } catch (SecurityException ignored) {
            }
            OutputStream os;
            if (logFile.equalsIgnoreCase(NO_LOG_FILE)) {
                os = new ByteArrayOutputStream();
            }
            else {
                os = new FileOutputStream(new File(logFile));
            }
            Field stream = SimpleLogger.class.getDeclaredField("TARGET_STREAM");
            stream.setAccessible(true);
            stream.set(null, new PrintStream(os));
        } catch (NoSuchFieldException | FileNotFoundException | IllegalAccessException e) {
            e.printStackTrace();
        }
        thread = new Thread(() -> {
            PrintStream out;
            try {
                out = new PrintStream(System.out, true, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                out = System.out;
            }
            Ansi ansi = Ansi.ansi();
            boolean running = true;
            while (running) {
                if (finished) {
                    running = false;
                }
                if (error) {
                    ansi.fgRed();
                }
                else {
                    ansi.fgGreen();
                }
                String s = ansi.eraseLine(Ansi.Erase.ALL)
                        .cursorToColumn(0)
                        .a(error ? FAILED : finished ? SUCCEEDED : ANIMATION[animationIndex])
                        .bold()
                        .a(status)
                        .reset()
                        .toString();
                out.print(s);
                out.flush();
                animationIndex++;
                if (animationIndex >= ANIMATION.length) {
                    animationIndex = 0;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
            }
        });
        thread.start();
    }

    @Override
    public void init(Context context)
            throws Exception {
    }

    @Override
    public void onEvent(Object event)
            throws Exception {
        try {
//            System.out.println(Ansi.ansi().fgBlue().a(event.getClass()).reset());
            if (event instanceof ExecutionEvent) {
                executionEventHandler((ExecutionEvent) event);
            }
            else if (event instanceof SettingsBuildingRequest) {
                settingsBuilderRequestEvent((SettingsBuildingRequest) event);
            }
            else if (event instanceof SettingsBuildingResult) {
                settingsBuilderResultEvent((SettingsBuildingResult) event);
            }
            else if (event instanceof MavenExecutionRequest) {
                executionRequestEventHandler((MavenExecutionRequest) event);
            }
            else if (event instanceof MavenExecutionResult) {
                executionResultEventHandler((MavenExecutionResult) event);
            }
            else if (event instanceof DependencyResolutionRequest) {
                dependencyResolutionRequestEvent((DependencyResolutionRequest) event);
            }
            else if (event instanceof DependencyResolutionResult) {
                dependencyResolutionResultEvent((DependencyResolutionResult) event);
            }
            else if (event instanceof RepositoryEvent) {
                repositoryEvent((RepositoryEvent) event);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        finished = true;
        thread.interrupt();
    }

    private void repositoryEvent(RepositoryEvent event) {
        if (error) {
            return;
        }
        if (event.getType() == RepositoryEvent.EventType.ARTIFACT_DOWNLOADING) {
            status = String.format("Downloading artifact %s:%s:%s", event.getArtifact()
                    .getGroupId(), event.getArtifact().getArtifactId(), event.getArtifact().getVersion());
            thread.interrupt();
        }
        else if (event.getType() == RepositoryEvent.EventType.ARTIFACT_DOWNLOADED) {
            status = String.format("Downloaded artifact %s:%s:%s", event.getArtifact()
                    .getGroupId(), event.getArtifact().getArtifactId(), event.getArtifact().getVersion());
            thread.interrupt();
        }
    }

    private void dependencyResolutionRequestEvent(DependencyResolutionRequest event) {
    }

    private void dependencyResolutionResultEvent(DependencyResolutionResult event) {
    }

    private void settingsBuilderRequestEvent(SettingsBuildingRequest event) {
    }

    private void settingsBuilderResultEvent(SettingsBuildingResult event) {
    }

    private void executionResultEventHandler(MavenExecutionResult event) {
        if (error) {
            return;
        }
        if (event.getExceptions() != null && !event.getExceptions().isEmpty()) {
            error = true;
            StringBuilder sb = new StringBuilder("Build failed\n");
            for (Throwable ex : event.getExceptions()) {
                sb.append(ex.getMessage()).append("\n");
            }
            status = sb.toString();
            thread.interrupt();
            return;
        }
        status = String.format("Finished %s:%s:%s", event.getProject()
                .getGroupId(), event.getProject().getArtifactId(), event.getProject()
                .getVersion());
        thread.interrupt();
    }

    private void executionRequestEventHandler(MavenExecutionRequest event) {
    }


    private void executionEventHandler(ExecutionEvent executionEvent) {
        if (error) {
            return;
        }
        switch (executionEvent.getType()) {
            case MojoFailed:
                status = String.format("Failed\n%s", executionEvent.getException().getMessage());
                error = true;
                thread.interrupt();
                break;
            case MojoStarted:
                status = String.format("Executing goal %s -> %s:%s",
                        executionEvent.getSession().getCurrentProject().getArtifactId(),
                        executionEvent.getMojoExecution().getArtifactId(),
                        executionEvent.getMojoExecution().getGoal());
                thread.interrupt();
                break;
            case MojoSucceeded:
                status = String.format("Finished goal %s -> %s:%s",
                        executionEvent.getSession().getCurrentProject().getArtifactId(),
                        executionEvent.getMojoExecution().getArtifactId(),
                        executionEvent.getMojoExecution().getGoal());
                thread.interrupt();
                break;
            case ProjectDiscoveryStarted:
                status = "Searching for projects...";
                thread.interrupt();
                break;
            case ProjectFailed:
                status = String.format("Project failed\n%s", executionEvent.getException().getMessage());
                error = true;
                thread.interrupt();
                break;
            case ProjectSucceeded:
                status = String.format("Project success %s:%s:%s", executionEvent.getProject()
                        .getGroupId(), executionEvent.getProject().getArtifactId(), executionEvent.getProject()
                        .getVersion());
                thread.interrupt();
                break;
            case SessionEnded:
            case MojoSkipped:
            case ForkFailed:
            case ForkStarted:
            case ForkSucceeded:
            case ForkedProjectFailed:
            case ForkedProjectStarted:
            case ForkedProjectSucceeded:
            case ProjectSkipped:
            case ProjectStarted:
            case SessionStarted:
            default:
                break;
        }
    }

}
