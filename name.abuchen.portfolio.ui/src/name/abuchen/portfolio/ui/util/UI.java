package name.abuchen.portfolio.ui.util;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.swt.widgets.Display;

/**
 * Utility class for marshalling work to the SWT UI thread.
 * 
 * This is critical for headless server scenarios where REST handlers run on
 * Jetty worker threads but need to access SWT resources (colors, fonts, etc.)
 * that must be accessed from the UI thread.
 */
public final class UI
{
    private UI()
    {
        // Utility class - prevent instantiation
    }

    /**
     * Execute a callable on the UI thread and return its result.
     * 
     * If already on the UI thread, executes immediately. Otherwise, marshals
     * the call via Display.syncExec() and waits for the result.
     * 
     * @param <T> the return type
     * @param task the task to execute
     * @return the result of the task
     * @throws IllegalStateException if Display is not initialized
     * @throws RuntimeException wrapping any exception thrown by the task
     */
    public static <T> T sync(Callable<T> task)
    {
        Display display = Display.getDefault();
        if (display == null)
            throw new IllegalStateException("Display not initialized");

        // If we're already on the UI thread, execute directly
        if (Display.getCurrent() == display)
        {
            try
            {
                return task.call();
            }
            catch (Exception e)
            {
                throw wrap(e);
            }
        }

        // Marshal to UI thread via syncExec
        final AtomicReference<T> result = new AtomicReference<>();
        final AtomicReference<RuntimeException> exception = new AtomicReference<>();

        display.syncExec(() -> {
            try
            {
                result.set(taskUncheck(task));
            }
            catch (RuntimeException e)
            {
                exception.set(e);
            }
        });

        if (exception.get() != null)
            throw exception.get();

        return result.get();
    }

    /**
     * Execute a runnable on the UI thread.
     * 
     * If already on the UI thread, executes immediately. Otherwise, marshals
     * the call via Display.syncExec().
     * 
     * @param run the runnable to execute
     * @throws IllegalStateException if Display is not initialized
     * @throws RuntimeException wrapping any exception thrown by the runnable
     */
    public static void sync(Runnable run)
    {
        sync(() -> {
            run.run();
            return null;
        });
    }

    /**
     * Execute a callable, converting checked exceptions to runtime exceptions.
     */
    private static <T> T taskUncheck(Callable<T> callable)
    {
        try
        {
            return callable.call();
        }
        catch (Exception e)
        {
            throw wrap(e);
        }
    }

    /**
     * Wrap a checked exception as a RuntimeException if necessary.
     */
    private static RuntimeException wrap(Exception e)
    {
        return (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException(e);
    }
}

