/*
 * This file is part of HoloAPI.
 *
 * HoloAPI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HoloAPI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with HoloAPI.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.goblom.injector.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class SafeField<T> implements FieldAccessor<T> {

    private Field field;
    private boolean isStatic;

    public SafeField(Field field) {
        setField(field);
    }

    public SafeField(Class<?> coreClass, String fieldName) {
        try {
            Field field = coreClass.getDeclaredField(fieldName);
            setField(field);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    protected void setField(Field field) {
        if (!field.isAccessible()) {
            try {
                field.setAccessible(true);
            } catch (Exception e) {
                e.printStackTrace();
                field = null;
            }
        }
        this.field = field;
        this.isStatic = Modifier.isStatic(field.getModifiers());
    }

    @Override
    public Field getField() {
        return this.field;
    }

    @Override
    public boolean set(Object instance, T value) {
        if (!isStatic && instance == null) {
            throw new UnsupportedOperationException("Non-static fields require a valid instance passed in!");
        }

        try {
            this.field.set(instance, value);
            return true;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public T get(Object instance) {
        if (!isStatic && instance == null) {
            throw new UnsupportedOperationException("Non-static fields require a valid instance passed in!");
        }
        try {
            return (T) this.field.get(instance);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public T transfer(Object from, Object to) {
        if (this.field == null) {
            return null;
        }
        T old = get(to);
        set(to, get(from));
        return old;
    }

    public String getName() {
        return this.field.getName();
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder(75);
        int mod = this.field.getModifiers();
        if (Modifier.isPublic(mod)) {
            string.append("public ");
        } else if (Modifier.isPrivate(mod)) {
            string.append("private ");
        } else if (Modifier.isProtected(mod)) {
            string.append("protected ");
        }

        if (Modifier.isStatic(mod)) {
            string.append("static ");
        }

        string.append(this.field.getName());

        return string.toString();
    }

    @Override
    public boolean isPublic() {
        return Modifier.isPublic(field.getModifiers());
    }

    @Override
    public boolean isReadOnly() {
        return Modifier.isFinal(field.getModifiers());
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        FieldAccessor<Integer> modifierField = new SafeField<Integer>(Field.class, "modifiers");

        if (readOnly) {
            modifierField.set(getField(), getField().getModifiers() | Modifier.FINAL);
        } else {
            modifierField.set(getField(), getField().getModifiers() & ~Modifier.FINAL);
        }
    }

    public static <T> T get(Class<?> clazz, String fieldname) {
        return new SafeField<T>(clazz, fieldname).get(null);
    }

    public static <T> T get(Object instance, String fieldName) {
        return new SafeField<T>(instance.getClass(), fieldName).get(instance);
    }

    public static <T> void set(Object instance, String fieldName, T value) {
        new SafeField<T>(instance.getClass(), fieldName).set(instance, value);
    }

    public static <T> void setStatic(Class<?> clazz, String fieldname, T value) {
        new SafeField<T>(clazz, fieldname).set(null, value);
    }
}