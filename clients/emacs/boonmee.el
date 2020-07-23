(defvar boonmee-command "boonmee")

(defun boonmee-handle-error (command)
  (print "Error"))

(defun boonmee-handle-definition (resp)
  (let* ((data (plist-get resp :data))
         (start (plist-get data :start))
         (line (plist-get start :line))
         (file (plist-get data :file)))
    (find-file file)
    ;; TODO: what about offsets?
    (goto-line line)))

(defun boonmee-handle-response (process output)
  (print output)
  (let* ((json-object-type 'plist)
         (resp (json-read-from-string output))
         (command (plist-get resp :command)))
    (cond
     ((string= "error" command)
      (boonmee-handle-error resp))

     ((string= "definition" command)
      (boonmee-handle-definition resp))

     (t (print "...")))))

(defun boonmee-init ()
  (if (get-process "boonmee")
      nil
    (let ((proc (start-process "boonmee" "boonmee-out" boonmee-command)))
      (set-process-filter proc 'boonmee-handle-response))))

(defun project-root (file)
  (print file)
  (let ((dir (file-name-directory file)))
    (if (file-exists-p (concat dir "node_modules"))
        dir
      (project-root dir))))

(defun boonmee-goto-definition ()
  (interactive)
  (let* ((line (string-to-number (format-mode-line "%l")))
         (offset (string-to-number (format-mode-line "%c")))
         (file (buffer-file-name))
         (req-id "1234")
         (args (list :file file :line line :offset offset :projectRoot (file-name-directory file)))
         (req (json-encode (list :command "definition" :type "request" :requestId req-id :arguments args))))
    (process-send-string "boonmee" (concat req "~\n"))))
