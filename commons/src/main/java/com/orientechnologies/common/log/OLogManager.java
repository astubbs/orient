package com.orientechnologies.common.log;

import com.orientechnologies.common.exception.OException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * @author Luca Garulli
 * @author Antony Stubbs
 */
public class OLogManager {

	private Logger libraryLogger = LoggerFactory.getLogger("org.orientechnologies");

	private static final OLogManager instance = new OLogManager();

	private Logger getLogger(final Object iRequester){
		if(iRequester == null)
			return libraryLogger;
		else
			return LoggerFactory.getLogger(iRequester.getClass());
	}

	public void debug(final Object iRequester, final String iMessage, final Object... iAdditionalArgs) {
		Logger logger = getLogger(iRequester);
		if(logger.isDebugEnabled()) {
			logger.debug(process(iMessage), iAdditionalArgs);
		}
	}

	private String process(String iMessage) {
		return iMessage.replaceAll("%s", "{}");
	}

	public void debug(final Object iRequester, final String iMessage, final Throwable iException, final Object... iAdditionalArgs) {
		Logger logger = getLogger(iRequester);
		if(logger.isDebugEnabled()) {
			logger.debug(process(iMessage), iException, iAdditionalArgs);
		}
	}

	public void info(final Object iRequester, final String iMessage, final Object... iAdditionalArgs) {
		Logger logger = getLogger(iRequester);
		if(logger.isInfoEnabled()) {
			logger.info(process(iMessage), iAdditionalArgs);
		}
	}

	public void info(final Object iRequester, final String iMessage, final Throwable iException, final Object... iAdditionalArgs) {
		Logger logger = getLogger(iRequester);
		if(logger.isInfoEnabled()) {
			logger.info(process(iMessage), iException, iAdditionalArgs);
		}
	}

	public void warn(final Object iRequester, final String iMessage, final Object... iAdditionalArgs) {
		Logger logger = getLogger(iRequester);
		if(logger.isWarnEnabled()) {
			logger.warn(process(iMessage), iAdditionalArgs);
		}
	}

	public void warn(final Object iRequester, final String iMessage, final Throwable iException, final Object... iAdditionalArgs) {
		Logger logger = getLogger(iRequester);
		if(logger.isWarnEnabled()) {
			logger.warn(process(iMessage), iAdditionalArgs, iException);
		}
	}

	public void error(final Object iRequester, final String iMessage, final Object... iAdditionalArgs) {
		Logger logger = getLogger(iRequester);
		if(logger.isErrorEnabled()) {
			logger.error(process(iMessage), iAdditionalArgs);
		}
	}

	public void error(final Object iRequester, final String iMessage, final Throwable iException, final Object... iAdditionalArgs) {
		Logger logger = getLogger(iRequester);
		if(logger.isErrorEnabled()) {
			logger.error(process(iMessage), iAdditionalArgs, iException);
		}
	}

	public void config(final Object iRequester, final String iMessage, final Object... iAdditionalArgs) {
		Logger logger = getLogger(iRequester);
		if(logger.isInfoEnabled()) {
			logger.info(process(iMessage), iAdditionalArgs);
		}
	}

	public void error(final Object iRequester, final String iMessage, final Throwable iException,
			final Class<? extends OException> iExceptionClass, final Object... iAdditionalArgs) {
		error(iRequester, iMessage, iException, iAdditionalArgs);

		try {
			throw iExceptionClass.getConstructor(String.class, Throwable.class).newInstance(iMessage, iException);
		} catch (NoSuchMethodException e) {
		} catch (IllegalArgumentException e) {
		} catch (SecurityException e) {
		} catch (InstantiationException e) {
		} catch (IllegalAccessException e) {
		} catch (InvocationTargetException e) {
		}
	}

	public void error(final Object iRequester, final String iMessage, final Class<? extends OException> iExceptionClass) {
		error(iRequester, iMessage, (Throwable) null);

		try {
			throw iExceptionClass.getConstructor(String.class).newInstance(iMessage);
		} catch (IllegalArgumentException e) {
		} catch (SecurityException e) {
		} catch (InstantiationException e) {
		} catch (IllegalAccessException e) {
		} catch (InvocationTargetException e) {
		} catch (NoSuchMethodException e) {
		}
	}

	@SuppressWarnings("unchecked")
	public void exception(final String iMessage, final Exception iNestedException, final Class<? extends OException> iExceptionClass,
			final Object... iAdditionalArgs) throws OException {
		if (iMessage == null)
			return;

		// FORMAT THE MESSAGE
		String msg = String.format(iMessage, iAdditionalArgs);

		Constructor<OException> c;
		OException exceptionToThrow = null;
		try {
			if (iNestedException != null) {
				c = (Constructor<OException>) iExceptionClass.getConstructor(String.class, Throwable.class);
				exceptionToThrow = c.newInstance(msg, iNestedException);
			}
		} catch (Exception e) {
		}

		if (exceptionToThrow == null)
			try {
				c = (Constructor<OException>) iExceptionClass.getConstructor(String.class);
				exceptionToThrow = c.newInstance(msg);
			} catch (SecurityException e1) {
			} catch (NoSuchMethodException e1) {
			} catch (IllegalArgumentException e1) {
			} catch (InstantiationException e1) {
			} catch (IllegalAccessException e1) {
			} catch (InvocationTargetException e1) {
			}

		if (exceptionToThrow != null)
			throw exceptionToThrow;
		else
			throw new IllegalArgumentException("Can't create the exception of type: " + iExceptionClass);
	}

	public static OLogManager instance() {
		return instance;
	}

  public boolean isDebugEnabled() {
    return this.libraryLogger.isDebugEnabled();
  }
}
